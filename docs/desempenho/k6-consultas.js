// Medição do RNF04 (Issue #58): p95 das consultas RF06, RF09 e RF15 sobre o dataset de 1.000 solicitações.
//
// Uma execução por FASE (variável FASE), todas contra a mesma aplicação já em pé, nesta ordem:
//   aquecimento     1 VU, 40 iterações. Só aquece JIT, pool e caches; NÃO conta para nenhum resultado oficial.
//   principal       1 VU, 200 iterações. Cada iteração faz uma consulta de cada tipo, então são 200 por tipo.
//   complementar    10 VUs durante 30 s, as mesmas três consultas (carga concorrente).
//
// Cada requisição medida leva as tags etapa=medicao, consulta=RF06|RF09|RF15 e variante=...; as do setup() levam
// etapa=preparo e ficam fora do p95 geral.
// As metas (p95 < 2.000 ms) são limiares do k6: se algum falhar, o k6 termina com código 99.
// Respostas de erro NÃO contam como sucesso: cada requisição tem verificação do status HTTP e do conteúdo,
// e o limiar de checks exige 100% (uma resposta 4xx/5xx rápida reprova a medição, não a melhora).
//
// Exemplo (veja docs/desempenho/README.md): docker run --rm -i -e FASE=principal grafana/k6:<versão> run - < k6-consultas.js

import http from 'k6/http';
import { check, fail } from 'k6';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const FASE = __ENV.FASE || 'principal';
const META_MS = 2000;
const ADMIN = { headers: { 'X-Perfil': 'ADMINISTRADOR', 'X-Usuario-Id': '1' } };

const MUNICIPIOS = ['Lagarto', 'Aracaju', 'Itabaiana', 'Estância', 'São Cristóvão',
    'Nossa Senhora do Socorro', 'Simão Dias', 'Tobias Barreto'];
const JANELAS = [['2025-11-01', '2025-12-31'], ['2026-01-01', '2026-02-28'], ['2026-03-01', '2026-04-30'],
    ['2026-05-01', '2026-06-30'], ['2026-07-01', '2026-08-31'], ['2026-09-01', '2026-10-31']];
const SITUACOES = ['CONCLUIDA', 'SOLICITADA', 'AGENDADA', 'RECUSADA', 'CANCELADA', 'EM_RECOLHIMENTO'];

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
}

export const options = {
    scenarios: { [FASE]: CENARIOS[FASE] },
    thresholds: limiares,
    summaryTrendStats: ['min', 'avg', 'med', 'p(90)', 'p(95)', 'max'],
};

// Antes de medir: confirma que o banco tem o dataset PERF completo e descobre os ids reais dos pontos
export function setup() {
    const pontos = http.get(`${BASE}/pontos-coleta?nome=PERF&size=100`, { tags: { etapa: 'preparo' } });
    const sol = http.get(`${BASE}/solicitacoes?size=1`, { ...ADMIN, tags: { etapa: 'preparo' } });
    if (pontos.status !== 200 || sol.status !== 200) {
        fail(`API indisponível (pontos=${pontos.status}, solicitações=${sol.status}).`);
    }
    const totalPontos = pontos.json('totalElementos');
    const totalSol = sol.json('totalElementos');
    if (totalPontos !== 20 || totalSol !== 1000) {
        fail(`Banco errado ou dataset ausente: pontos PERF=${totalPontos} (esperado 20), solicitações=${totalSol} (esperado 1000).`);
    }
    return { pontoIds: pontos.json('conteudo').map((p) => p.id) };
}

function verificar(res, rotulo, conteudoOk) {
    return check(res, {
        [`${rotulo}: status 200`]: (r) => r.status === 200,
        [`${rotulo}: conteúdo esperado`]: (r) => {
            try { return conteudoOk(r.json()); } catch (e) { return false; }
        },
    });
}

export default function (dados) {
    const n = __ITER; // varia os filtros a cada iteração, de modo determinístico

    // RF06: pesquisa pública de pontos (sem cabeçalhos de ator)
    const BAIRROS = ['Centro', 'Jardim', 'Industrial', 'Santa Maria', 'Siqueira Campos'];
    const rf06 = n % 2 === 0
        ? { variante: 'municipio', url: `/pontos-coleta?municipio=${encodeURIComponent(MUNICIPIOS[n % 8])}&size=20` }
        : { variante: 'bairro', url: `/pontos-coleta?nome=PERF&bairro=${encodeURIComponent(BAIRROS[n % 5])}&size=20` };
    const r6 = http.get(`${BASE}${rf06.url}`, { tags: { etapa: 'medicao', consulta: 'RF06', variante: rf06.variante } });
    verificar(r6, 'RF06', (j) => Array.isArray(j.conteudo) && j.conteudo.length > 0 && j.totalElementos > 0);

    // RF09: listagem administrativa de solicitações, quatro variantes de filtro em rodízio
    let rf09;
    switch (n % 4) {
        case 0: rf09 = { variante: 'situacao', url: `/solicitacoes?situacao=${SITUACOES[n % 6]}&size=50` }; break;
        case 1: rf09 = { variante: 'ponto', url: `/solicitacoes?pontoId=${dados.pontoIds[n % dados.pontoIds.length]}&size=50` }; break;
        case 2: {
            const [de, ate] = JANELAS[n % JANELAS.length];
            rf09 = { variante: 'periodo', url: `/solicitacoes?de=${de}&ate=${ate}&size=50` };
            break;
        }
        default: rf09 = { variante: 'pagina', url: `/solicitacoes?page=${n % 10}&size=100` };
    }
    const r9 = http.get(`${BASE}${rf09.url}`, { ...ADMIN, tags: { etapa: 'medicao', consulta: 'RF09', variante: rf09.variante } });
    verificar(r9, 'RF09', (j) => Array.isArray(j.conteudo) && j.conteudo.length > 0 && j.totalElementos > 0);

    // RF15: resumo gerencial, com e sem período
    let rf15;
    if (n % 2 === 0) {
        rf15 = { variante: 'geral', url: '/resumo-gerencial' };
    } else {
        const [de, ate] = JANELAS[n % JANELAS.length];
        rf15 = { variante: 'periodo', url: `/resumo-gerencial?de=${de}&ate=${ate}` };
    }
    const r15 = http.get(`${BASE}${rf15.url}`, { ...ADMIN, tags: { etapa: 'medicao', consulta: 'RF15', variante: rf15.variante } });
    verificar(r15, 'RF15', (j) => Array.isArray(j.solicitacoesPorSituacao) && Array.isArray(j.quantidadesRecolhidas)
        && j.solicitacoesPorSituacao.length > 0 && j.quantidadesRecolhidas.length > 0);
}
