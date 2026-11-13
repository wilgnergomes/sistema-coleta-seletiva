/// Integração HTTP com a API real, pelo proxy de mesma origem.
///
/// Usa os repositórios do aplicativo e o banco de demonstração do perfil
/// `demo` (dados fictícios do seed). Cria solicitações novas e não altera
/// cadastros do seed de forma permanente.
///
/// Execução, com o backend e o Caddy no ar (veja o README):
///   dart test test_integracao
/// Base diferente: `API_INTEGRACAO=http://127.0.0.1:8000/api dart test test_integracao`
@TestOn('vm')
library;

import 'dart:io';

import 'package:coleta_seletiva_web/core/api/cliente_api.dart';
import 'package:coleta_seletiva_web/core/api/falhas.dart';
import 'package:coleta_seletiva_web/core/dominio/datas.dart';
import 'package:coleta_seletiva_web/core/dominio/enums.dart';
import 'package:coleta_seletiva_web/core/dominio/quantidade.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/modelos.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/repositorio_catalogo.dart';
import 'package:coleta_seletiva_web/features/moradores/data/moradores.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/data/solicitacoes.dart';
import 'package:http/http.dart' as http;
import 'package:test/test.dart';

final base = Uri.parse(Platform.environment['API_INTEGRACAO'] ?? 'http://127.0.0.1:8000/api');

ClienteApi cliente([Map<String, String> identidade = const {}]) =>
    ClienteApi(base: base, http: http.Client(), identidade: () => identidade);

Map<String, String> morador(int id) => {'X-Perfil': 'MORADOR', 'X-Usuario-Id': '$id'};
Map<String, String> operador(int id) => {'X-Perfil': 'OPERADOR', 'X-Usuario-Id': '$id'};
const administrador = {'X-Perfil': 'ADMINISTRADOR'};

/// IDs do seed de demonstração, localizados pelos nomes e e-mails.
late int maria;
late int joao;
late int carla;
late int rita;
late PontoColeta verdeVida;
late MaterialAceito papel;
late MaterialAceito pilhas;
late int enderecoMaria;

/// Ponto ativo com recolhimento, um material ativo que ele não aceita e um
/// material que ele aceita.
late (int, int, int) incompativel;

Future<ErroHttp> falha(Future<Object?> acao) async {
  try {
    await acao;
  } on ErroHttp catch (e) {
    return e;
  }
  fail('a chamada deveria falhar');
}

void main() {
  final fuso = FusoNegocio('America/Sao_Paulo');

  setUpAll(() async {
    try {
      await cliente().get('/pontos-coleta', query: {'size': 1});
    } on FalhaApi catch (e) {
      fail('API indisponível em $base: ${e.mensagem}');
    }
    await fuso.garantir();
    final admin = RepositorioMoradores(cliente(administrador));
    final moradores = (await admin.listar(tamanho: 100)).conteudo;
    maria = moradores.firstWhere((m) => m.email == 'maria.silva@exemplo.com').id;
    joao = moradores.firstWhere((m) => m.email == 'joao.pereira@exemplo.com').id;
    final ops = await cliente(administrador).get('/operadores', query: {'size': 100}, acesso: Acesso.ator);
    final listaOps = (ops! as Map)['conteudo'] as List;
    int opId(String email) => (listaOps.firstWhere((o) => (o as Map)['email'] == email) as Map)['id'] as int;
    carla = opId('carla.nunes@exemplo.com');
    rita = opId('rita.alves@exemplo.com');
    final catalogo = RepositorioCatalogo(cliente());
    verdeVida = (await catalogo.pesquisarPontos(const FiltroPontos(nome: 'Verde Vida'))).conteudo.single;
    papel = verdeVida.materiaisAceitos.firstWhere((m) => m.nomeMaterial == 'Papel');
    pilhas = verdeVida.materiaisAceitos.firstWhere((m) => m.unidade == Unidade.unidade);
    final ativos = (await catalogo.materiais(tamanho: 100)).conteudo;
    final domiciliares = (await catalogo.pesquisarPontos(
      const FiltroPontos(),
      tamanho: 100,
    )).conteudo.where((p) => p.aceitaSolicitacao);
    incompativel = [
      for (final p in domiciliares)
        for (final m in ativos)
          if (!p.materiaisAceitos.any((v) => v.materialId == m.id) && p.materiaisAceitos.isNotEmpty)
            (p.id, m.id, p.materiaisAceitos.first.materialId),
    ].first;
    enderecoMaria = (await RepositorioMoradores(cliente(morador(maria))).enderecos(maria)).first.id;
  });

  RepositorioSolicitacoes solicitacoes(Map<String, String> ator) => RepositorioSolicitacoes(cliente(ator));

  Future<SolicitacaoDetalhe> novaSolicitacao({Quantidade? quantidade}) => solicitacoes(morador(maria)).criar(
    pontoId: verdeVida.id,
    enderecoId: enderecoMaria,
    itens: [ItemPedido(papel.materialId, quantidade ?? const Quantidade.milesimos(2000))],
    observacoes: 'Teste de integração do frontend',
  );

  DateTime futuro(int dias, int hora) {
    final hoje = fuso.hoje(DateTime.now());
    final d = DateTime.utc(hoje.ano, hoje.mes, hoje.dia).add(Duration(days: dias));
    return fuso.instante(DataCivil(d.year, d.month, d.day), hora, 0);
  }

  test('RF06: filtro por município e material devolve só pontos ativos que aceitam o material', () async {
    final catalogo = RepositorioCatalogo(cliente());
    final r = await catalogo.pesquisarPontos(FiltroPontos(municipio: 'lagarto', materialId: papel.materialId));
    expect(r.conteudo, isNotEmpty);
    for (final p in r.conteudo) {
      expect(p.ativo, isTrue);
      expect(p.endereco.municipio.toLowerCase(), 'lagarto');
      expect(p.materiaisAceitos.map((m) => m.materialId), contains(papel.materialId));
    }
    final semAcento = await catalogo.pesquisarPontos(const FiltroPontos(municipio: 'Itabaiana', bairro: 'Marcela'));
    expect(semAcento.conteudo.every((p) => p.endereco.bairro == 'Marcela'), isTrue);
  });

  test('RF01/RNF06: morador não consulta outro morador; catálogo público não expõe dados de moradores', () async {
    final e = await falha(RepositorioMoradores(cliente(morador(joao))).obter(maria));
    expect(e.status, anyOf(403, 404));
    final publico = await cliente().get('/pontos-coleta', query: {'size': 1});
    expect(publico.toString(), isNot(contains('@exemplo.com')));
  });

  test('RN01: material fora do ponto é recusado sem gravação parcial', () async {
    final antes = (await solicitacoes(morador(maria)).listar(const FiltroSolicitacoes())).totalElementos;
    final e = await falha(
      solicitacoes(morador(maria)).criar(
        pontoId: incompativel.$1,
        enderecoId: enderecoMaria,
        itens: [
          ItemPedido(incompativel.$3, const Quantidade.milesimos(1000)),
          ItemPedido(incompativel.$2, const Quantidade.milesimos(1000)),
        ],
      ),
    );
    expect(e.status, anyOf(400, 409));
    final depois = (await solicitacoes(morador(maria)).listar(const FiltroSolicitacoes())).totalElementos;
    expect(depois, antes);
  });

  test('RN02: peça fracionária é recusada no item', () async {
    final e = await falha(
      solicitacoes(morador(maria)).criar(
        pontoId: verdeVida.id,
        enderecoId: enderecoMaria,
        itens: [ItemPedido(pilhas.materialId, const Quantidade.milesimos(1500))],
      ),
    );
    expect(e.status, anyOf(400, 409));
    expect(e.mensagem, isNotEmpty);
  });

  test('RF10: agendamento no passado é recusado e a solicitação continua SOLICITADA', () async {
    final s = await novaSolicitacao();
    final repo = solicitacoes(operador(carla));
    final e = await falha(repo.agendar(s.id, DateTime.now().toUtc().subtract(const Duration(hours: 1)), s.versao));
    expect(e.status, 400);
    expect(e.errosPorCampo.keys, contains('dataHora'));
    expect((await repo.detalhe(s.id)).situacao, SituacaoSolicitacao.solicitada);
    await solicitacoes(morador(maria)).cancelar(s.id, 'Limpeza do teste de integração', s.versao);
  });

  test('RF07/RF09: operador de outro ponto não vê a solicitação; administrador não muda situação', () async {
    final s = await novaSolicitacao();
    expect((await falha(solicitacoes(operador(rita)).detalhe(s.id))).status, 404);
    expect((await falha(solicitacoes(administrador).agendar(s.id, futuro(3, 9), s.versao))).status, 403);
    await solicitacoes(morador(maria)).cancelar(s.id, 'Limpeza do teste de integração', s.versao);
  });

  test('RF08–RF14 e RN06: fluxo completo com conflito de versão, falha e conclusão', () async {
    final m = solicitacoes(morador(maria));
    final o = solicitacoes(operador(carla));
    var s = await novaSolicitacao(quantidade: const Quantidade.milesimos(3500));
    expect(s.protocolo, startsWith('SOL-'));
    expect(s.historico.single.situacaoNova, SituacaoSolicitacao.solicitada);
    expect(s.itens.single.unidade, Unidade.kg);

    s = await m.corrigir(
      s.id,
      enderecoId: enderecoMaria,
      itens: [ItemPedido(papel.materialId, const Quantidade.milesimos(4000))],
      versao: s.versao,
    );
    expect(s.itens.single.quantidadeEstimada.texto, '4');
    expect(s.historico, hasLength(1), reason: 'correção não cria linha de situação');

    s = await o.agendar(s.id, futuro(2, 9), s.versao);
    expect(s.situacao, SituacaoSolicitacao.agendada);

    // Dois contextos leem a mesma versão; o segundo envio fica desatualizado.
    final a = await o.detalhe(s.id);
    final b = await o.detalhe(s.id);
    await o.reagendar(a.id, futuro(3, 10), 'Ajuste de rota', a.versao);
    final conflito = await falha(o.reagendar(b.id, futuro(4, 10), 'Outro ajuste', b.versao));
    expect(conflito.conflitoVersao, isTrue);

    s = await o.detalhe(s.id);
    s = await o.iniciar(s.id, s.versao);
    s = await o.registrarFalha(
      s.id,
      motivo: 'Morador ausente',
      informacoesTentativa: 'Portão fechado',
      novaDataHora: futuro(5, 9),
      versao: s.versao,
    );
    expect(s.situacao, SituacaoSolicitacao.agendada);
    s = await o.iniciar(s.id, s.versao);
    final todasZero = await falha(o.concluir(s.id, {papel.materialId: Quantidade.zero}, s.versao));
    expect(todasZero.status, anyOf(400, 409));
    s = await o.concluir(s.id, {papel.materialId: const Quantidade.milesimos(3750)}, s.versao);
    expect(s.situacao, SituacaoSolicitacao.concluida);
    expect(s.concluidaEm, isNotNull);
    expect(s.itens.single.quantidadeEfetiva?.texto, '3,75');
    expect(s.historico.map((h) => h.situacaoNova), [
      SituacaoSolicitacao.solicitada,
      SituacaoSolicitacao.agendada,
      SituacaoSolicitacao.agendada,
      SituacaoSolicitacao.emRecolhimento,
      SituacaoSolicitacao.agendada,
      SituacaoSolicitacao.emRecolhimento,
      SituacaoSolicitacao.concluida,
    ]);
    // O morador lê o mesmo histórico; ações finais não são mais aceitas.
    expect((await m.detalhe(s.id)).situacao, SituacaoSolicitacao.concluida);
    expect((await falha(m.cancelar(s.id, 'tarde demais', s.versao))).status, 409);
  });

  test('RN05: ponto com solicitação aberta não pode ser inativado', () async {
    final s = await novaSolicitacao();
    final e = await falha(
      RepositorioCatalogo(cliente(administrador)).salvarPonto(
        id: verdeVida.id,
        nome: verdeVida.nome,
        endereco: verdeVida.endereco,
        recolhimentoDomiciliar: verdeVida.recolhimentoDomiciliar,
        situacao: 'INATIVO',
        contato: verdeVida.contato,
        horarios: verdeVida.horarios,
      ),
    );
    expect(e.conflitoRegra, isTrue);
    expect((await RepositorioCatalogo(cliente()).ponto(verdeVida.id, forcar: true)).ativo, isTrue);
    await solicitacoes(morador(maria)).cancelar(s.id, 'Limpeza do teste de integração', s.versao);
  });

  test('RF15: resumo com as seis situações e unidade por material', () async {
    final json = await cliente(administrador).get('/resumo-gerencial', acesso: Acesso.ator);
    final mapa = json! as Map;
    expect((mapa['solicitacoesPorSituacao'] as List).map((s) => (s as Map)['situacao']), [
      'SOLICITADA',
      'AGENDADA',
      'EM_RECOLHIMENTO',
      'CONCLUIDA',
      'RECUSADA',
      'CANCELADA',
    ]);
    for (final q in mapa['quantidadesRecolhidas'] as List) {
      expect((q as Map)['unidade'], isIn(['KG', 'L', 'UN']));
    }
  });

  test('RNF03: cabeçalho de perfil inválido recebe 403 com corpo padrão', () async {
    final e = await falha(cliente(const {'X-Perfil': 'VISITANTE'}).get('/solicitacoes', acesso: Acesso.ator));
    expect(e.status, 403);
    expect(e.codigo, 'ACESSO_NEGADO');
  });
}
