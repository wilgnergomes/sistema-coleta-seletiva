import 'package:coleta_seletiva_web/app/app.dart';
import 'package:coleta_seletiva_web/app/rotas.dart';
import 'package:coleta_seletiva_web/app/sessao.dart';
import 'package:coleta_seletiva_web/features/moradores/data/moradores.dart';
import 'package:coleta_seletiva_web/features/moradores/ui/admin_moradores.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/marca.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'api_falsa.dart';
import 'morador_test.dart' show dependencias, enderecoJson, moradorJson;

ApiFalsa apiPublica() => ApiFalsa()
  ..quando(
    'GET /materiais',
    (_) => (
      200,
      pagina([
        materialJson(6, 'Pilhas e baterias', unidade: 'UN'),
        materialJson(4, 'Metal'),
        materialJson(1, 'Papel'),
        materialJson(5, 'Óleo de cozinha', unidade: 'L'),
      ]),
    ),
  )
  ..quando('GET /pontos-coleta', (_) => (200, pagina([pontoJson(4, 'Cooperativa Verde Vida')])))
  ..quando('GET /moradores/3', (_) => (200, moradorJson(3)))
  ..quando('GET /moradores/3/enderecos', (_) => (200, [enderecoJson(7, 3)]))
  ..quando('GET /solicitacoes', (_) => (200, pagina([])));

Future<GoRouter> abrir(
  WidgetTester tester, {
  String modo = 'demo',
  Ator? ator,
  String rota = '/',
  ApiFalsa? api,
}) async {
  final d = dependencias(api ?? apiPublica(), modo: modo);
  if (ator != null) d.sessao.entrar(ator);
  final rotas = criarRotas(d);
  await tester.pumpWidget(AppColetaSeletiva(rotas: rotas));
  rotas.go(rota);
  await tester.pumpAndSettle();
  return rotas;
}

void main() {
  group('página inicial pública', () {
    testWidgets('abre em /, com seções distintas e os materiais reais', (tester) async {
      final semantica = tester.ensureSemantics();
      definirTela(tester, computador);
      final rotas = await abrir(tester);
      expect(rotas.state.uri.path, '/');
      expect(find.bySemanticsLabel('Reciclar ficou mais simples.'), findsOneWidget);
      for (final trecho in [
        'Do separar ao recolher',
        'Cada material',
        'O ponto vai',
        'Onde levar os seus recicláveis?',
        'Separe hoje.',
      ]) {
        await tester.scrollUntilVisible(
          find.textContaining(trecho).last,
          300,
          scrollable: find.byType(Scrollable).first,
        );
        expect(find.textContaining(trecho), findsWidgets);
      }
      // Os tradicionais ganham blocos; os demais aparecem como "Também aceitos".
      expect(find.bySemanticsLabel('Ver orientações de Papel'), findsOneWidget);
      expect(find.bySemanticsLabel('Ver orientações de Metal'), findsOneWidget);
      expect(find.text('Também aceitos:'), findsOneWidget);
      expect(find.widgetWithText(ActionChip, 'Pilhas e baterias'), findsOneWidget);
      expect(find.byType(SimboloReciclagem), findsWidgets);
      expect(tester.takeException(), isNull);
      semantica.dispose();
    });

    testWidgets('ação principal e links levam às rotas reais', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester);
      await tester.tap(find.text('Encontrar um ponto de coleta'));
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/pontos');
      rotas.go('/');
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.text('Experimentar a demonstração').first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Experimentar a demonstração').first);
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/demonstracao');
    });

    testWidgets('filtros da página inicial abrem o catálogo com o painel de filtros', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester);
      await tester.scrollUntilVisible(find.text('Filtros').last, 300, scrollable: find.byType(Scrollable).first);
      await tester.enterText(find.widgetWithText(TextField, 'Nome do ponto de coleta'), 'Verde');
      await tester.ensureVisible(find.text('Filtros').last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Filtros').last);
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/pontos');
      expect(rotas.state.uri.queryParameters.containsKey('filtros'), isFalse);
      expect(find.text('Aplicar filtros'), findsOneWidget);
      expect(find.widgetWithText(TextField, 'Verde'), findsWidgets);
    });

    testWidgets('compilação pública não oferece demonstração; 360 px sem transbordo', (tester) async {
      definirTela(tester, celular);
      await abrir(tester, modo: 'publico');
      expect(find.text('Experimentar a demonstração'), findsNothing);
      expect(find.text('Encontrar um ponto de coleta'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  });

  group('saída da demonstração', () {
    testWidgets('sair limpa a identidade e volta à página inicial', (tester) async {
      definirTela(tester, computador);
      final api = apiPublica();
      final rotas = await abrir(
        tester,
        api: api,
        ator: const Ator(Perfil.morador, 3, nome: 'Maria Silva'),
        rota: '/morador/conta',
      );
      expect(find.textContaining('Simulação de identidade · Morador #3'), findsOneWidget);
      await tester.tap(find.text('Sair da demonstração').first);
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/');
      expect(find.textContaining('Simulação de identidade'), findsNothing);
      // Uma área restrita volta a exigir a escolha de identidade.
      rotas.go('/morador/conta');
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/demonstracao');
    });

    testWidgets('trocar perfil limpa o contexto antes da nova seleção', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(
        tester,
        ator: const Ator(Perfil.morador, 3, nome: 'Maria Silva'),
        rota: '/morador/conta',
      );
      await tester.tap(find.text('Trocar perfil').first);
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/demonstracao');
      expect(find.text('Contexto ativo'), findsNothing);
    });

    testWidgets('no celular as opções ficam no menu da faixa', (tester) async {
      definirTela(tester, celular);
      final rotas = await abrir(tester, ator: const Ator(Perfil.morador, 3), rota: '/morador/conta');
      await tester.tap(find.byTooltip('Opções da demonstração'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Sair da demonstração').last);
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/');
    });
  });

  testWidgets('moradores: total da API, cartões sem dados pessoais e abertura pelo número', (tester) async {
    final api = ApiFalsa()..quando('GET /moradores', (_) => (200, pagina([moradorJson(3), moradorJson(4)], total: 57)));
    int? aberto;
    definirTela(tester, computador);
    await tester.pumpWidget(
      comTema(
        Scaffold(
          body: PaginaMoradoresAdmin(
            repositorio: RepositorioMoradores(api.cliente(identidade: () => {'X-Perfil': 'ADMINISTRADOR'})),
            pagina: 0,
            aoMudarPagina: (_) {},
            aoAbrir: (id) async => aberto = id,
            aoCadastrar: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('57 moradores cadastrados'), findsOneWidget);
    expect(find.text('maria.silva@exemplo.com'), findsNothing);
    expect(find.text('Morador #3'), findsOneWidget);
    await tester.enterText(find.widgetWithText(TextField, 'Abrir morador pelo número'), '42');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();
    expect(aberto, 42);
  });
}
