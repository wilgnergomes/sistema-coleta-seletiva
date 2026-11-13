import 'dart:convert';

import 'package:coleta_seletiva_web/core/dominio/datas.dart';
import 'package:coleta_seletiva_web/core/dominio/enums.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/repositorio_catalogo.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/data/solicitacoes.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/ui/fila_operador.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/ui/view_models.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'api_falsa.dart';
import 'solicitacoes_morador_test.dart' show detalheJson, resumoJson;

/// Botão (de qualquer variante, inclusive `.icon`) que contém o texto.
Finder botao(String texto, {Type? tipo}) => find.ancestor(
  of: find.text(texto),
  matching: find.byWidgetPredicate((w) => tipo == FilledButton ? w is FilledButton : w is ButtonStyleButton),
);

/// 13/11/2026 13:00 em São Paulo.
DateTime agoraFixo() => DateTime.utc(2026, 11, 13, 16);

class Cenario {
  Cenario(this.api);

  final ApiFalsa api;
  late final cliente = api.cliente(identidade: () => {'X-Perfil': 'OPERADOR', 'X-Usuario-Id': '1'});
  late final repositorio = RepositorioSolicitacoes(cliente);
  late final catalogo = RepositorioCatalogo(cliente);

  DetalheSolicitacaoViewModel detalhe(int id) => DetalheSolicitacaoViewModel(
    repositorio: repositorio,
    catalogo: catalogo,
    fuso: FusoNegocio('America/Sao_Paulo'),
    id: id,
  );

  ListaSolicitacoesViewModel lista() =>
      ListaSolicitacoesViewModel(repositorio: repositorio, catalogo: catalogo, fuso: FusoNegocio('America/Sao_Paulo'));
}

Future<void> abrirPainel(WidgetTester tester, Cenario c, {int id = 3, Size tela = computador}) async {
  definirTela(tester, tela);
  await tester.pumpWidget(
    comTema(
      Scaffold(
        body: PainelDetalheOperador(viewModel: c.detalhe(id)..carregar().ignore(), aoAlterar: () {}, agora: agoraFixo),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  group('ações por situação', () {
    for (final (situacao, esperadas, ausentes) in [
      ('SOLICITADA', ['Agendar', 'Recusar', 'Cancelar'], ['Iniciar recolhimento']),
      ('AGENDADA', ['Cancelar'], ['Agendar', 'Recusar']),
      ('CONCLUIDA', <String>[], ['Agendar', 'Cancelar']),
    ]) {
      testWidgets(situacao, (tester) async {
        final c = Cenario(ApiFalsa()..quando('GET /solicitacoes/3', (_) => (200, detalheJson(3, situacao: situacao))));
        await abrirPainel(tester, c);
        for (final rotulo in esperadas) {
          expect(botao(rotulo), findsOneWidget, reason: rotulo);
        }
        for (final rotulo in ausentes) {
          expect(botao(rotulo), findsNothing, reason: rotulo);
        }
        if (esperadas.isEmpty) expect(find.text('Situação final: não há ações disponíveis.'), findsOneWidget);
      });
    }
  });

  testWidgets('agendar: data e hora no fuso de negócio, instante UTC e versão lida', (tester) async {
    Map<String, Object?>? corpo;
    final c = Cenario(
      ApiFalsa()
        ..quando('GET /solicitacoes/3', (_) => (200, detalheJson(3, versao: 2)))
        ..quando('POST /solicitacoes/3/agendamento', (r) {
          corpo = jsonDecode(r.body) as Map<String, Object?>;
          return (200, detalheJson(3, situacao: 'AGENDADA', versao: 3));
        }),
    );
    await abrirPainel(tester, c);
    await tester.tap(botao('Agendar'));
    await tester.pumpAndSettle();

    // Hoje às 09:00 já passou (agora são 13:00): recusado antes do envio.
    await tester.tap(find.text('Escolher data *'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Escolher hora *'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    await tester.tap(botao('Agendar', tipo: FilledButton).last);
    await tester.pumpAndSettle();
    expect(find.text('Escolha uma data e hora futuras.'), findsOneWidget);
    expect(c.api.chamadas('POST /solicitacoes/3/agendamento'), isEmpty);

    await tester.tap(find.text('Data: 13/11/2026'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('20'));
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    await tester.tap(botao('Agendar', tipo: FilledButton).last);
    await tester.pumpAndSettle();
    expect(corpo, {'dataHora': '2026-11-20T12:00:00.000Z', 'versao': 2});
    expect(find.text('Recolhimento agendado.'), findsOneWidget);
    expect(find.text('Agendada'), findsWidgets);
  });

  testWidgets('recusar exige motivo e envia a versão', (tester) async {
    Map<String, Object?>? corpo;
    final c = Cenario(
      ApiFalsa()
        ..quando('GET /solicitacoes/3', (_) => (200, detalheJson(3, versao: 1)))
        ..quando('POST /solicitacoes/3/recusa', (r) {
          corpo = jsonDecode(r.body) as Map<String, Object?>;
          return (200, detalheJson(3, situacao: 'RECUSADA', versao: 2));
        }),
    );
    await abrirPainel(tester, c);
    await tester.tap(botao('Recusar'));
    await tester.pumpAndSettle();
    await tester.tap(botao('Recusar solicitação', tipo: FilledButton));
    await tester.pumpAndSettle();
    expect(find.text('Informe motivo.'), findsOneWidget);
    await tester.enterText(find.byType(TextFormField), 'Material fora do padrão');
    await tester.tap(botao('Recusar solicitação', tipo: FilledButton));
    await tester.pumpAndSettle();
    expect(corpo, {'motivo': 'Material fora do padrão', 'versao': 1});
    expect(find.text('Situação final: não há ações disponíveis.'), findsOneWidget);
  });

  testWidgets('solicitação de outro ponto (404) aparece como indisponível', (tester) async {
    final c = Cenario(ApiFalsa());
    await abrirPainel(tester, c, id: 99);
    expect(find.text('Solicitação indisponível'), findsOneWidget);
  });

  testWidgets('compacto: mais ações agrupa as secundárias', (tester) async {
    final c = Cenario(ApiFalsa()..quando('GET /solicitacoes/3', (_) => (200, detalheJson(3))));
    await abrirPainel(tester, c, tela: celular);
    expect(botao('Agendar', tipo: FilledButton), findsOneWidget);
    expect(find.text('Mais ações'), findsOneWidget);
    expect(botao('Recusar'), findsNothing);
    await tester.tap(find.text('Mais ações'));
    await tester.pumpAndSettle();
    expect(find.text('Recusar'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  group('fila', () {
    Cenario cenario() => Cenario(
      ApiFalsa()
        ..quando('GET /pontos-coleta', (_) => (200, pagina([pontoJson(4, 'Cooperativa Verde Vida')], tamanho: 100)))
        ..quando(
          'GET /solicitacoes',
          (r) => (200, pagina([resumoJson(3, situacao: r.url.queryParameters['situacao'] ?? 'AGENDADA')])),
        )
        ..quando('GET /solicitacoes/3', (_) => (200, detalheJson(3))),
    );

    Widget fila(Cenario c, ListaSolicitacoesViewModel lista, {int? selecionado, void Function(int)? aoSelecionar}) =>
        comTema(
          Scaffold(
            body: PaginaFila(
              lista: lista,
              filtro: const FiltroSolicitacoes(situacao: SituacaoSolicitacao.solicitada),
              selecionado: selecionado,
              criarDetalhe: c.detalhe,
              aoMudarFiltro: (_) {},
              aoSelecionar: aoSelecionar ?? (_) {},
              aoFecharDetalhe: () {},
            ),
          ),
        );

    testWidgets('1366 px: lista e detalhe lado a lado, com a consulta da situação', (tester) async {
      final c = cenario();
      definirTela(tester, computador);
      await tester.pumpWidget(fila(c, c.lista(), selecionado: 3));
      await tester.pumpAndSettle();
      expect(find.text('1 solicitação no filtro'), findsOneWidget);
      expect(find.text('Ver histórico'), findsOneWidget);
      expect(find.text('Cooperativa Verde Vida'), findsWidgets);
      final consulta = c.api.chamadas('GET /solicitacoes').first;
      expect(consulta.url.queryParameters['situacao'], 'SOLICITADA');
      expect(consulta.headers['X-Perfil'], 'OPERADOR');
    });

    testWidgets('360 px: lista primeiro; com seleção, o detalhe ocupa a tela', (tester) async {
      final c = cenario();
      final lista = c.lista();
      int? aberto;
      definirTela(tester, celular);
      await tester.pumpWidget(fila(c, lista, aoSelecionar: (id) => aberto = id));
      await tester.pumpAndSettle();
      expect(find.text('Fila do ponto'), findsOneWidget);
      await tester.tap(find.textContaining('SOL-20261005-SEED03'));
      expect(aberto, 3);
      await tester.pumpWidget(fila(c, lista, selecionado: 3));
      await tester.pumpAndSettle();
      expect(find.text('Voltar para fila'), findsOneWidget);
      expect(find.text('Fila do ponto'), findsNothing);
      expect(tester.takeException(), isNull);
    });
  });
}
