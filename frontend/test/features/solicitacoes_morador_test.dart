import 'dart:convert';

import 'package:coleta_seletiva_web/core/api/falhas.dart';
import 'package:coleta_seletiva_web/core/dominio/datas.dart';
import 'package:coleta_seletiva_web/core/dominio/enums.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/modelos.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/repositorio_catalogo.dart';
import 'package:coleta_seletiva_web/features/moradores/data/moradores.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/data/solicitacoes.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/ui/formulario_solicitacao.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/ui/recentes_morador.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/ui/solicitacoes_morador.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/ui/view_models.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'api_falsa.dart';
import 'morador_test.dart' show enderecoJson;

Map<String, Object?> resumoJson(int id, {int pontoId = 4, String situacao = 'SOLICITADA', int versao = 0}) => {
  'id': id,
  'protocolo': 'SOL-20261005-SEED${id.toString().padLeft(2, '0')}',
  'moradorId': 3,
  'pontoId': pontoId,
  'enderecoCopia': 'Rua das Flores, 120 - Centro - Lagarto/SE - CEP 49400000',
  'situacao': situacao,
  'criadaEm': '2026-10-05T12:00:00Z',
  'agendadaPara': null,
  'concluidaEm': null,
  'observacoes': null,
  'versao': versao,
};

Map<String, Object?> detalheJson(
  int id, {
  String situacao = 'SOLICITADA',
  int versao = 0,
  List<Map<String, Object?>>? itens,
}) => {
  ...resumoJson(id, situacao: situacao, versao: versao),
  'itens':
      itens ??
      [
        {
          'materialId': 1,
          'nomeMaterial': 'Papel',
          'unidade': 'KG',
          'quantidadeEstimada': 12.5,
          'quantidadeEfetiva': null,
        },
      ],
  'historico': [
    {
      'id': 1,
      'situacaoAnterior': null,
      'situacaoNova': 'SOLICITADA',
      'data': '2026-10-05T12:00:00Z',
      'responsavel': {'tipo': 'MORADOR', 'id': 3},
      'motivo': null,
      'agendamento': null,
      'informacoesTentativa': null,
    },
  ],
};

ApiFalsa apiFormulario() => ApiFalsa()
  ..quando('GET /moradores/3/enderecos', (_) => (200, [enderecoJson(7, 3), enderecoJson(8, 3, municipio: 'Aracaju')]))
  ..quando(
    'GET /materiais',
    (_) => (200, pagina([materialJson(1, 'Papel'), materialJson(6, 'Pilhas e baterias', unidade: 'UN')])),
  )
  ..quando('GET /pontos-coleta', (_) => (200, pagina([pontoJson(4, 'Cooperativa Verde Vida')])))
  ..quando(
    'GET /pontos-coleta/4',
    (_) => (
      200,
      pontoJson(
        4,
        'Cooperativa Verde Vida',
        materiais: [
          vinculoJson(1, 'Papel', limite: 100),
          vinculoJson(6, 'Pilhas e baterias', unidade: 'UN'),
        ],
      ),
    ),
  );

void main() {
  late FusoNegocio fuso;

  setUpAll(() async {
    fuso = FusoNegocio('America/Sao_Paulo');
    await fuso.garantir();
  });

  group('datas no fuso de negócio', () {
    test('combina dia e hora de parede e serializa o instante', () {
      final instante = fuso.instante(const DataCivil(2026, 12, 15), 9, 0);
      expect(instante.toIso8601String(), '2026-12-15T12:00:00.000Z');
      expect(fuso.dataHora(DateTime.parse('2026-10-05T12:00:00Z')), '05/10/2026 09:00');
      expect(fuso.data(DateTime.parse('2026-10-06T02:30:00Z')), '05/10/2026');
    });

    test('data civil valida o calendário', () {
      expect(DataCivil.ler('2026-02-30'), isNull);
      expect(DataCivil.ler('2026-02-28')?.iso, '2026-02-28');
    });
  });

  test('FiltroSolicitacoes preserva TODAS quando o padrão é uma situação', () {
    const f = FiltroSolicitacoes(de: DataCivil(2026, 10, 1), pagina: 2);
    final url = f.paraUrl(situacaoPadrao: SituacaoSolicitacao.solicitada);
    expect(url['situacao'], 'TODAS');
    expect(FiltroSolicitacoes.deQuery(url, situacaoPadrao: SituacaoSolicitacao.solicitada), f);
    expect(
      FiltroSolicitacoes.deQuery({}, situacaoPadrao: SituacaoSolicitacao.solicitada).situacao,
      SituacaoSolicitacao.solicitada,
    );
    expect(f.paraApi(), containsPair('de', '2026-10-01'));
  });

  group('pontos usados recentemente', () {
    final pontos = {
      4: PontoColeta.deJson(pontoJson(4, 'Cooperativa Verde Vida')),
      2: PontoColeta.deJson(pontoJson(2, 'Recicla Itabaiana', municipio: 'Itabaiana')),
      3: PontoColeta.deJson(pontoJson(3, 'Ecoponto Centro', domiciliar: false)),
      5: PontoColeta.deJson(pontoJson(5, 'Comunitário')),
    };
    final lidos = <int>[];
    Future<PontoColeta> ler(int id) async {
      lidos.add(id);
      final p = pontos[id];
      if (p == null) throw const ErroHttp(status: 404, codigo: 'NAO_ENCONTRADO', mensagem: 'x');
      return p;
    }

    List<SolicitacaoResumo> pedidos(List<int> ids) => [
      for (var i = 0; i < ids.length; i++) SolicitacaoResumo.deJson(resumoJson(i + 1, pontoId: ids[i])),
    ];

    setUp(lidos.clear);

    test('sem repetição, no máximo dois, ignorando inativos e sem recolhimento', () async {
      final r = await pontosRecentes(pedidos([9, 4, 4, 3, 5, 2]), ler);
      expect(r.map((p) => p.id), [4, 5]);
      expect(lidos, [9, 4, 3, 5]);
    });

    test('filtra pelo município do endereço selecionado', () async {
      final r = await pontosRecentes(pedidos([4, 2]), ler, municipio: 'itabaiana');
      expect(r.map((p) => p.id), [2]);
    });

    test('limita as consultas de ponto e não reserva espaço sem resultado', () async {
      final r = await pontosRecentes(pedidos([9, 10, 11, 12, 4]), ler);
      expect(r, isEmpty);
      expect(lidos, hasLength(maximoConsultasPonto));
    });
  });

  group('formulário de solicitação', () {
    Widget tela(ApiFalsa api, {int? solicitacaoId, void Function(SolicitacaoDetalhe)? aoConcluir}) {
      final cliente = api.cliente();
      return comTema(
        Scaffold(
          body: PaginaFormularioSolicitacao(
            moradorId: 3,
            solicitacaoId: solicitacaoId,
            pontoInicial: solicitacaoId == null ? 4 : null,
            solicitacoes: RepositorioSolicitacoes(cliente),
            catalogo: RepositorioCatalogo(cliente),
            moradores: RepositorioMoradores(cliente),
            aoConcluir: (r, {required criada}) => aoConcluir?.call(r),
            aoVoltar: () {},
            aoConsultarPontos: () {},
          ),
        ),
      );
    }

    Future<void> escolherMaterial(WidgetTester tester, String nome, {int linha = 0}) async {
      await tester.tap(find.byType(DropdownButtonFormField<int>).at(1 + linha));
      await tester.pumpAndSettle();
      await tester.tap(find.text(nome).last);
      await tester.pumpAndSettle();
    }

    testWidgets('cria com ponto da rota, endereço e quantidade em pt-BR', (tester) async {
      Map<String, Object?>? corpo;
      final api = apiFormulario()
        ..quando('POST /solicitacoes', (r) {
          corpo = jsonDecode(r.body) as Map<String, Object?>;
          return (201, detalheJson(20));
        });
      SolicitacaoDetalhe? criada;
      definirTela(tester, computador);
      await tester.pumpWidget(tela(api, aoConcluir: (r) => criada = r));
      await tester.pumpAndSettle();
      expect(find.text('Cooperativa Verde Vida'), findsOneWidget);
      await escolherMaterial(tester, 'Papel');
      await tester.enterText(find.widgetWithText(TextFormField, 'Quantidade estimada (kg) *'), '3,5');
      await tester.ensureVisible(find.text('Enviar solicitação'));
      await tester.tap(find.text('Enviar solicitação'));
      await tester.pumpAndSettle();
      expect(corpo, {
        'pontoId': 4,
        'enderecoId': 7,
        'observacoes': null,
        'itens': [
          {'materialId': 1, 'quantidadeEstimada': 3.5},
        ],
      });
      expect(criada?.id, 20);
    });

    testWidgets('recusa fração em peças e limite excedido antes de enviar', (tester) async {
      final api = apiFormulario();
      definirTela(tester, computador);
      await tester.pumpWidget(tela(api));
      await tester.pumpAndSettle();
      await escolherMaterial(tester, 'Pilhas e baterias');
      await tester.enterText(find.widgetWithText(TextFormField, 'Quantidade estimada (un.) *'), '2,5');
      await tester.ensureVisible(find.text('Enviar solicitação'));
      await tester.tap(find.text('Enviar solicitação'));
      await tester.pumpAndSettle();
      expect(find.text('Este material é contado em unidades inteiras.'), findsOneWidget);

      await tester.tap(find.byType(DropdownButtonFormField<int>).at(1));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Papel').last);
      await tester.pumpAndSettle();
      await tester.enterText(find.widgetWithText(TextFormField, 'Quantidade estimada (kg) *'), '150');
      await tester.ensureVisible(find.text('Enviar solicitação'));
      await tester.tap(find.text('Enviar solicitação'));
      await tester.pumpAndSettle();
      expect(find.text('O limite deste ponto é 100 kg.'), findsOneWidget);
      expect(api.chamadas('POST /solicitacoes'), isEmpty);
    });

    testWidgets('erro do servidor em itens[0] aparece na linha e preserva o rascunho', (tester) async {
      final api = apiFormulario()
        ..quando(
          'POST /solicitacoes',
          (_) => (
            400,
            {
              'codigo': 'VALIDACAO',
              'mensagem': 'Há campos inválidos.',
              'erros': [
                {'campo': 'itens[0].quantidadeEstimada', 'mensagem': 'Quantidade acima do limite do ponto.'},
              ],
            },
          ),
        );
      definirTela(tester, computador);
      await tester.pumpWidget(tela(api));
      await tester.pumpAndSettle();
      await escolherMaterial(tester, 'Papel');
      await tester.enterText(find.widgetWithText(TextFormField, 'Quantidade estimada (kg) *'), '10');
      await tester.ensureVisible(find.text('Enviar solicitação'));
      await tester.tap(find.text('Enviar solicitação'));
      await tester.pumpAndSettle();
      expect(find.text('Quantidade acima do limite do ponto.'), findsOneWidget);
      expect(find.text('10'), findsOneWidget);
    });

    testWidgets('correção: conflito de versão relê, preserva a edição e reenvia com a nova versão', (tester) async {
      var versaoAtual = 0;
      final versoesEnviadas = <Object?>[];
      final api = apiFormulario()
        ..quando('GET /solicitacoes/5', (_) => (200, detalheJson(5, versao: versaoAtual)))
        ..quando('PUT /solicitacoes/5', (r) {
          final corpo = jsonDecode(r.body) as Map<String, Object?>;
          versoesEnviadas.add(corpo['versao']);
          if (corpo['versao'] != versaoAtual) {
            return (409, {'codigo': 'CONFLITO_VERSAO', 'mensagem': 'A solicitação foi alterada.', 'erros': []});
          }
          return (200, detalheJson(5, versao: versaoAtual + 1));
        });
      SolicitacaoDetalhe? salva;
      definirTela(tester, computador);
      await tester.pumpWidget(tela(api, solicitacaoId: 5, aoConcluir: (r) => salva = r));
      await tester.pumpAndSettle();
      expect(find.text('Rua das Flores, 120 - Centro - Lagarto/SE - CEP 49400000'), findsOneWidget);

      // A correção exige escolha explícita do endereço atual.
      await tester.ensureVisible(find.text('Salvar correção'));
      await tester.tap(find.text('Salvar correção'));
      await tester.pumpAndSettle();
      expect(find.text('Escolha o endereço.'), findsOneWidget);

      await tester.tap(find.byType(DropdownButtonFormField<int>).first);
      await tester.pumpAndSettle();
      await tester.tap(find.textContaining('Aracaju').last);
      await tester.pumpAndSettle();
      await tester.enterText(find.widgetWithText(TextFormField, 'Quantidade estimada (kg) *'), '20');

      versaoAtual = 1; // Outra ação alterou a solicitação.
      await tester.ensureVisible(find.text('Salvar correção'));
      await tester.tap(find.text('Salvar correção'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Seus dados foram mantidos'), findsOneWidget);
      expect(find.text('20'), findsOneWidget);

      await tester.ensureVisible(find.text('Salvar correção'));
      await tester.tap(find.text('Salvar correção'));
      await tester.pumpAndSettle();
      expect(versoesEnviadas, [0, 1]);
      expect(salva?.versao, 2);
    });
  });

  group('detalhe do morador', () {
    Future<DetalheSolicitacaoViewModel> abrir(WidgetTester tester, ApiFalsa api) async {
      final cliente = api.cliente();
      final vm = DetalheSolicitacaoViewModel(
        repositorio: RepositorioSolicitacoes(cliente),
        catalogo: RepositorioCatalogo(cliente),
        // Criado na zona do teste: um Future concluído em outra zona não
        // avança dentro do tempo simulado do testWidgets.
        fuso: FusoNegocio('America/Sao_Paulo'),
        id: 5,
      );
      definirTela(tester, celular);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaDetalheSolicitacaoMorador(viewModel: vm, aoVoltar: () {}, aoCorrigir: (_) {}),
          ),
        ),
      );
      await tester.pumpAndSettle();
      return vm;
    }

    testWidgets('ações por situação e cancelamento com motivo e versão', (tester) async {
      Map<String, Object?>? corpo;
      final api = ApiFalsa()
        ..quando('GET /pontos-coleta/4', (_) => (200, pontoJson(4, 'Cooperativa Verde Vida')))
        ..quando('GET /solicitacoes/5', (_) => (200, detalheJson(5, situacao: 'AGENDADA', versao: 3)))
        ..quando('POST /solicitacoes/5/cancelamento', (r) {
          corpo = jsonDecode(r.body) as Map<String, Object?>;
          return (200, detalheJson(5, situacao: 'CANCELADA', versao: 4));
        });
      await abrir(tester, api);
      expect(find.text('Cooperativa Verde Vida'), findsOneWidget);
      expect(find.text('Corrigir solicitação'), findsNothing);
      expect(find.text('12,5 kg'), findsNothing);
      expect(find.text('Estimada: 12,5 kg'), findsOneWidget);
      expect(find.text('Recolhida: ainda não informada'), findsOneWidget);
      await tester.ensureVisible(find.text('Cancelar solicitação'));
      await tester.tap(find.text('Cancelar solicitação'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField), '  Viagem  ');
      await tester.tap(find.widgetWithText(FilledButton, 'Cancelar solicitação'));
      await tester.pumpAndSettle();
      expect(corpo, {'motivo': 'Viagem', 'versao': 3});
      expect(find.text('Solicitação cancelada.'), findsOneWidget);
      expect(find.text('Cancelar solicitação'), findsNothing);
      expect(tester.takeException(), isNull);
    });

    testWidgets('conflito de regra relê o detalhe e retira ações inválidas', (tester) async {
      var situacao = 'SOLICITADA';
      final api = ApiFalsa()
        ..quando('GET /solicitacoes/5', (_) => (200, detalheJson(5, situacao: situacao)))
        ..quando('POST /solicitacoes/5/cancelamento', (_) {
          situacao = 'EM_RECOLHIMENTO';
          return (
            409,
            {'codigo': 'CONFLITO_REGRA', 'mensagem': 'A solicitação não pode ser cancelada agora.', 'erros': []},
          );
        });
      await abrir(tester, api);
      await tester.ensureVisible(find.text('Cancelar solicitação'));
      await tester.tap(find.text('Cancelar solicitação'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField), 'Desisti');
      await tester.tap(find.widgetWithText(FilledButton, 'Cancelar solicitação'));
      await tester.pumpAndSettle();
      expect(find.text('A solicitação não pode ser cancelada agora.'), findsOneWidget);
      expect(find.text('Em recolhimento'), findsWidgets);
      expect(find.text('Cancelar solicitação'), findsNothing);
      expect(api.chamadas('GET /solicitacoes/5'), hasLength(2));
    });

    testWidgets('404 retira o conteúdo e oferece voltar', (tester) async {
      await abrir(tester, ApiFalsa());
      expect(find.text('Solicitação indisponível'), findsOneWidget);
    });
  });
}
