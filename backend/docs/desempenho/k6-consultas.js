// Medição do RNF04 (Issue #58): p95 das consultas RF06, RF09 e RF15 sobre o dataset de 1.000 solicitações.
//
// Uma execução por FASE (variável FASE), todas contra a mesma aplicação já em pé, nesta ordem:
//   aquecimento     1 VU, 40 iterações. Só aquece JIT, pool e caches; NÃO conta para nenhum resultado oficial.
//   principal       1 VU, 200 iterações. Cada iteração faz uma consulta de cada tipo, então são 200 por tipo.
//   complementar    10 VUs durante 30 s, as mesmas três consultas (carga concorrente).
//
// Em cada iteração, cada tipo de consulta escolhe UMA variante em rodízio (n = número da iteração):
//   RF06 (5 variantes): município, bairro, material, filtros combinados, paginação.
//   RF09 (10 variantes): ADMINISTRADOR (situação, ponto, período, página sem filtro), MORADOR e OPERADOR
//                        (sem filtro, por situação, por período). O contexto do ator vira condição fixa do WHERE.
//   RF15 (2 variantes): geral e por período.
// Com 200 iterações, cada variante de RF06 roda 40 vezes, cada uma de RF09 roda 20 e cada uma de RF15 roda 100.
// O parâmetro de cada variante (qual município, qual ponto, qual morador...) avança a cada volta do rodízio.
//
// Nenhum id é fixo: o setup() descobre pelos próprios endpoints os pontos, materiais, moradores e operadores do
// dataset PERF, então a medição vale depois de limpar e recarregar a base (os ids mudam, a carga é a mesma).
//
// Cada requisição medida leva as tags etapa=medicao, consulta=RF06|RF09|RF15, variante=... e perfil=...; as do
// setup() levam etapa=preparo e ficam fora do p95 geral. As metas (p95 < 2.000 ms) são limiares do k6: se algum
// falhar, o k6 termina com código 99. Respostas de erro NÃO contam como sucesso: cada requisição verifica o status
// HTTP e o conteúdo, e o limiar de checks exige 100%.
//
// Exemplo (veja docs/desempenho/README.md): docker run --rm -i -e FASE=principal grafana/k6:<versão> run - < k6-consultas.js

import http from 'k6/http';
import { check, fail } from 'k6';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const FASE = __ENV.FASE || 'principal';
const META_MS = 2000;

const JANELAS = [['2025-11-01', '2025-12-31'], ['2026-01-01', '2026-02-28'], ['2026-03-01', '2026-04-30'],
    ['2026-05-01', '2026-06-30'], ['2026-07-01', '2026-08-31'], ['2026-09-01', '2026-10-31']];
const SITUACOES = ['CONCLUIDA', 'SOLICITADA', 'AGENDADA', 'RECUSADA', 'CANCELADA', 'EM_RECOLHIMENTO'];

// Dataset: 200 moradores com 5 solicitações cada; 20 pontos com 50 solicitações cada
const SOLICITACOES_POR_MORADOR = 5;
const SOLICITACOES_POR_PONTO = 50;

const VARIANTES_RF06 = ['municipio', 'bairro', 'material', 'combinado', 'paginacao'];

const VARIANTES_RF09 = [
    ['situacao', 'ADMINISTRADOR'], ['ponto', 'ADMINISTRADOR'], ['periodo', 'ADMINISTRADOR'], ['pagina', 'ADMINISTRADOR'],
    ['geral', 'MORADOR'], ['situacao', 'MORADOR'], ['periodo', 'MORADOR'],
    ['geral', 'OPERADOR'], ['situacao', 'OPERADOR'], ['periodo', 'OPERADOR'],
];

const CENARIOS = {
    aquecimento: { executor: 'per-vu-iterations', vus: 1, iterations: 40, maxDuration: '5m' },
    principal: { executor: 'per-vu-iterations', vus: 1, iterations: 200, maxDuration: '10m' },
    complementar: { executor: 'constant-vus', vus: 10, duration: '30s' },
};
if (!CENARIOS[FASE]) {
    fail(`FASE inválida: ${FASE}. Use aquecimento, principal ou complementar.`);
}

const limiares = {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
};
// Limiares só nas fases oficiais; o aquecimento não é medido
if (FASE !== 'aquecimento') {
    limiares['http_req_duration{etapa:medicao}'] = [`p(95)<${META_MS}`];
    for (const consulta of ['RF06', 'RF09', 'RF15']) {
        limiares[`http_req_duration{consulta:${consulta}}`] = [`p(95)<${META_MS}`];
    }
    // Um limiar por variante: faz o k6 exportar o p95 de cada uma, base da comparação com a V3
    for (const v of VARIANTES_RF06) {
        limiares[`http_req_duration{consulta:RF06,variante:${v}}`] = [`p(95)<${META_MS}`];
    }
    for (const [v, perfil] of VARIANTES_RF09) {
        limiares[`http_req_duration{consulta:RF09,variante:${v},perfil:${perfil}}`] = [`p(95)<${META_MS}`];
    }
    for (const v of ['geral', 'periodo']) {
        limiares[`http_req_duration{consulta:RF15,variante:${v}}`] = [`p(95)<${META_MS}`];
    }
}

export const options = {
    scenarios: { [FASE]: CENARIOS[FASE] },
    thresholds: limiares,
    summaryTrendStats: ['min', 'avg', 'med', 'p(90)', 'p(95)', 'max'],
};

const ADMIN = { 'X-Perfil': 'ADMINISTRADOR', 'X-Usuario-Id': '1' };
const PREPARO = { etapa: 'preparo' };

function obter(caminho, cabecalhos) {
    const res = http.get(`${BASE}${caminho}`, { headers: cabecalhos || {}, tags: PREPARO });
    if (res.status !== 200) {
        fail(`API indisponível ou resposta inesperada em ${caminho}: status ${res.status}.`);
    }
    return res.json();
}

// Percorre todas as páginas de uma listagem (até 10) e devolve os itens
function todos(caminho, cabecalhos) {
    const itens = [];
    for (let pagina = 0; pagina < 10; pagina++) {
        const corpo = obter(`${caminho}${caminho.includes('?') ? '&' : '?'}size=100&page=${pagina}`, cabecalhos);
        itens.push(...corpo.conteudo);
        if (pagina + 1 >= corpo.totalPaginas) break;
    }
    return itens;
}

// Antes de medir: confirma que o banco tem o dataset PERF completo e descobre os ids reais (nunca fixos)
export function setup() {
    const totalSolicitacoes = obter('/solicitacoes?size=1', ADMIN).totalElementos;
    const pontos = todos('/pontos-coleta?nome=PERF', {});
    const moradores = todos('/moradores', ADMIN).filter((m) => m.email.startsWith('perf.morador.'));
    const operadores = todos('/operadores', ADMIN).filter((o) => o.email.startsWith('perf.operador.'));
    if (totalSolicitacoes !== 1000 || pontos.length !== 20 || moradores.length !== 200 || operadores.length !== 20) {
        fail(`Banco errado ou dataset ausente: solicitações=${totalSolicitacoes} (esperado 1000), pontos PERF=${pontos.length} (20), `
            + `moradores PERF=${moradores.length} (200), operadores PERF=${operadores.length} (20).`);
    }
    const idsPontos = new Set(pontos.map((p) => p.id));
    if (!operadores.every((o) => idsPontos.has(o.pontoId))) {
        fail('Operador PERF vinculado a ponto fora do dataset.');
    }
    // Materiais realmente aceitos por algum ponto (ids vindos dos vínculos, não de uma lista fixa)
    const materiais = [...new Set(pontos.flatMap((p) => p.materiaisAceitos.map((m) => m.materialId)))];
    return {
        pontos: pontos.map((p) => ({
            id: p.id,
            municipio: p.endereco.municipio,
            bairro: p.endereco.bairro,
            materialId: p.materiaisAceitos[0].materialId,
        })),
        materiais,
        moradorIds: moradores.map((m) => m.id),
        operadorIds: operadores.map((o) => o.id),
    };
}

function verificar(res, rotulo, conteudoOk) {
    return check(res, {
        [`${rotulo}: status 200`]: (r) => r.status === 200,
        [`${rotulo}: conteúdo esperado`]: (r) => {
            try { return conteudoOk(r.json()); } catch (e) { return false; }
        },
    });
}

const naoVazia = (j) => Array.isArray(j.conteudo) && j.conteudo.length > 0 && j.totalElementos > 0;
// Filtro que pode legitimamente não achar nada dentro do contexto do ator: exige formato e limite coerentes
const dentroDoLimite = (limite) => (j) => Array.isArray(j.conteudo) && j.totalElementos >= 0 && j.totalElementos <= limite
    && j.conteudo.length <= j.totalElementos;
const totalExato = (esperado) => (j) => Array.isArray(j.conteudo) && j.totalElementos === esperado && j.conteudo.length > 0;

// ---------------------------------------------------------------------------------------------------------
// RF06: pesquisa pública de pontos (sem cabeçalhos de ator)
// ---------------------------------------------------------------------------------------------------------

function rf06(n, dados) {
    const k = VARIANTES_RF06.length;
    const variante = VARIANTES_RF06[n % k];
    const q = Math.floor(n / k);
    const ponto = dados.pontos[q % dados.pontos.length];
    let url;
    let conteudoOk = naoVazia;
    switch (variante) {
        case 'municipio':
            url = `/pontos-coleta?municipio=${encodeURIComponent(ponto.municipio)}&size=20`;
            break;
        case 'bairro':
            url = `/pontos-coleta?nome=PERF&bairro=${encodeURIComponent(ponto.bairro)}&size=20`;
            break;
        case 'material':
            url = `/pontos-coleta?nome=PERF&materialId=${dados.materiais[q % dados.materiais.length]}&size=20`;
            break;
        case 'combinado':
            // município + bairro + material de um ponto existente: o próprio ponto satisfaz todos os filtros
            url = `/pontos-coleta?nome=PERF&municipio=${encodeURIComponent(ponto.municipio)}`
                + `&bairro=${encodeURIComponent(ponto.bairro)}&materialId=${ponto.materialId}&size=20`;
            conteudoOk = (j) => naoVazia(j) && j.conteudo.some((p) => p.id === ponto.id);
            break;
        default:
            // 20 pontos em páginas de 5: as páginas 0 a 3 são completas (nunca uma página além do fim)
            url = `/pontos-coleta?nome=PERF&size=5&page=${q % 4}`;
            conteudoOk = (j) => j.totalElementos === 20 && j.conteudo.length === 5;
    }
    const res = http.get(`${BASE}${url}`, { tags: { etapa: 'medicao', consulta: 'RF06', variante, perfil: 'PUBLICO' } });
    verificar(res, 'RF06', conteudoOk);
}

// ---------------------------------------------------------------------------------------------------------
// RF09: listagem de solicitações no contexto de ADMINISTRADOR, MORADOR e OPERADOR
// ---------------------------------------------------------------------------------------------------------

function rf09(n, dados) {
    const k = VARIANTES_RF09.length;
    const [variante, perfil] = VARIANTES_RF09[n % k];
    const q = Math.floor(n / k);
    const [de, ate] = JANELAS[q % JANELAS.length];
    const situacao = SITUACOES[q % SITUACOES.length];

    let cabecalhos;
    let limite;
    if (perfil === 'ADMINISTRADOR') {
        cabecalhos = ADMIN;
    } else if (perfil === 'MORADOR') {
        cabecalhos = { 'X-Perfil': 'MORADOR', 'X-Usuario-Id': String(dados.moradorIds[q % dados.moradorIds.length]) };
        limite = SOLICITACOES_POR_MORADOR;
    } else {
        cabecalhos = { 'X-Perfil': 'OPERADOR', 'X-Usuario-Id': String(dados.operadorIds[q % dados.operadorIds.length]) };
        limite = SOLICITACOES_POR_PONTO;
    }

    let url;
    let conteudoOk;
    if (perfil === 'ADMINISTRADOR') {
        conteudoOk = naoVazia;
        if (variante === 'situacao') url = `/solicitacoes?situacao=${situacao}&size=50`;
        else if (variante === 'ponto') url = `/solicitacoes?pontoId=${dados.pontos[q % dados.pontos.length].id}&size=50`;
        else if (variante === 'periodo') url = `/solicitacoes?de=${de}&ate=${ate}&size=50`;
        else url = `/solicitacoes?page=${q % 10}&size=100`;
    } else if (variante === 'geral') {
        // Sem filtro, o ator enxerga exatamente as suas: 5 do morador, 50 do ponto do operador
        url = '/solicitacoes?size=50';
        conteudoOk = totalExato(limite);
    } else if (variante === 'situacao') {
        url = `/solicitacoes?situacao=${situacao}&size=50`;
        conteudoOk = dentroDoLimite(limite);
    } else {
        url = `/solicitacoes?de=${de}&ate=${ate}&size=50`;
        conteudoOk = dentroDoLimite(limite);
    }
    const res = http.get(`${BASE}${url}`, { headers: cabecalhos, tags: { etapa: 'medicao', consulta: 'RF09', variante, perfil } });
    verificar(res, 'RF09', conteudoOk);
}

// ---------------------------------------------------------------------------------------------------------
// RF15: resumo gerencial, com e sem período
// ---------------------------------------------------------------------------------------------------------
function rf15(n) {
    let variante;
    let url;
    if (n % 2 === 0) {
        variante = 'geral';
        url = '/resumo-gerencial';
    } else {
        variante = 'periodo';
        const [de, ate] = JANELAS[Math.floor(n / 2) % JANELAS.length];
        url = `/resumo-gerencial?de=${de}&ate=${ate}`;
    }
    const res = http.get(`${BASE}${url}`, { headers: ADMIN, tags: { etapa: 'medicao', consulta: 'RF15', variante, perfil: 'ADMINISTRADOR' } });
    verificar(res, 'RF15', (j) => Array.isArray(j.solicitacoesPorSituacao) && Array.isArray(j.quantidadesRecolhidas)
        && j.solicitacoesPorSituacao.length > 0 && j.quantidadesRecolhidas.length > 0);
}

export default function (dados) {
    const n = __ITER; // varia as consultas a cada iteração, de modo determinístico
    rf06(n, dados);
    rf09(n, dados);
    rf15(n);
}
