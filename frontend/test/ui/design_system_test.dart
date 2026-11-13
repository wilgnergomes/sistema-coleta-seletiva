import 'package:coleta_seletiva_web/app/tema/tema.dart';
import 'package:coleta_seletiva_web/app/tema/tokens.dart';
import 'package:coleta_seletiva_web/core/api/falhas.dart';
import 'package:coleta_seletiva_web/core/api/pagina.dart';
import 'package:coleta_seletiva_web/core/dominio/enums.dart';
import 'package:coleta_seletiva_web/core/estado/carga.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/estados.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/material_visual.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/paginador.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/shell.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';

const _destinosMorador = [
  DestinoNavegacao(rotulo: 'Início', icone: Icons.home_outlined, iconeSelecionado: Icons.home, rota: '/a'),
  DestinoNavegacao(rotulo: 'Pontos', icone: Icons.place_outlined, iconeSelecionado: Icons.place, rota: '/b'),
  DestinoNavegacao(
    rotulo: 'Solicitações',
    icone: Icons.list_alt_outlined,
    iconeSelecionado: Icons.list_alt,
    rota: '/c',
  ),
  DestinoNavegacao(rotulo: 'Conta', icone: Icons.person_outline, iconeSelecionado: Icons.person, rota: '/d'),
];

Widget _shell({NavegacaoCompacta compacta = NavegacaoCompacta.barraInferior, ValueChanged<int>? aoSelecionar}) =>
    ShellAdaptativo(
      destinos: _destinosMorador,
      indiceSelecionado: 1,
      aoSelecionar: aoSelecionar ?? (_) {},
      compacta: compacta,
      titulo: const Text('Coleta Seletiva'),
      corpo: const Center(child: Text('conteúdo')),
    );

void main() {
  group('tokens', () {
    test('textos e controles atingem o contraste mínimo calculado', () {
      expect(contraste(Cores.texto, Cores.fundo), greaterThanOrEqualTo(4.5));
      expect(contraste(Cores.textoSecundario, Cores.fundo), greaterThanOrEqualTo(4.5));
      expect(contraste(Cores.sobrePrimario, Cores.primario), greaterThanOrEqualTo(4.5));
      expect(contraste(Cores.texto, Cores.destaque), greaterThanOrEqualTo(4.5));
      expect(contraste(Cores.contorno, Cores.fundo), greaterThanOrEqualTo(3));
      expect(contraste(Cores.foco, Cores.fundo), greaterThanOrEqualTo(3));
      expect(contraste(Cores.textoSecundario, Cores.desabilitadoFundo), greaterThanOrEqualTo(4.5));
    });

    test('todas as situações têm texto legível sobre o próprio fundo', () {
      const cores = CoresSituacao();
      for (final s in SituacaoSolicitacao.values) {
        final v = cores.solicitacao(s);
        expect(contraste(v.cor, v.fundo), greaterThanOrEqualTo(4.5), reason: s.name);
      }
    });

    test('ícones de material têm contraste de componente sobre a própria cor', () {
      for (final v in [
        VisualMaterial.papel,
        VisualMaterial.plastico,
        VisualMaterial.vidro,
        VisualMaterial.metal,
        VisualMaterial.perigoso,
        VisualMaterial.outro,
      ]) {
        expect(contraste(v.sobreCor, v.cor), greaterThanOrEqualTo(3));
      }
    });
  });

  test('visual do material segue a codificação CONAMA pelo nome cadastrado', () {
    expect(VisualMaterial.porNome('Papel'), VisualMaterial.papel);
    expect(VisualMaterial.porNome('PLÁSTICO'), VisualMaterial.plastico);
    expect(VisualMaterial.porNome(' vidro '), VisualMaterial.vidro);
    expect(VisualMaterial.porNome('Metal'), VisualMaterial.metal);
    expect(VisualMaterial.porNome('Pilhas e baterias'), VisualMaterial.perigoso);
    expect(VisualMaterial.porNome('Óleo de cozinha'), VisualMaterial.outro);
  });

  group('forma de navegação', () {
    test('segue os limiares de 600 e 1000 dp', () {
      FormaNavegacao forma(double largura, [NavegacaoCompacta c = NavegacaoCompacta.barraInferior]) =>
          formaNavegacao(largura: largura, quantidadeDestinos: 4, compacta: c);
      expect(forma(360), FormaNavegacao.barraInferior);
      expect(forma(599), FormaNavegacao.barraInferior);
      expect(forma(600), FormaNavegacao.trilho);
      expect(forma(999), FormaNavegacao.trilho);
      expect(forma(1000), FormaNavegacao.lateral);
      expect(forma(360, NavegacaoCompacta.gaveta), FormaNavegacao.gaveta);
    });

    test('barra inferior cede à gaveta com texto muito ampliado', () {
      expect(
        formaNavegacao(largura: 360, quantidadeDestinos: 4, compacta: NavegacaoCompacta.barraInferior, escalaTexto: 2),
        FormaNavegacao.gaveta,
      );
    });

    test('um único destino dispensa trilho e barra', () {
      expect(
        formaNavegacao(largura: 1366, quantidadeDestinos: 1, compacta: NavegacaoCompacta.cabecalho),
        FormaNavegacao.cabecalho,
      );
    });
  });

  group('shell adaptativo', () {
    testWidgets('360 px: barra inferior com quatro destinos', (tester) async {
      definirTela(tester, celular);
      await tester.pumpWidget(comTema(_shell()));
      expect(find.byType(NavigationBar), findsOneWidget);
      expect(find.byType(NavigationRail), findsNothing);
      expect(find.text('Solicitações'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('1366 px: navegação lateral estendida', (tester) async {
      definirTela(tester, computador);
      int? escolhido;
      await tester.pumpWidget(comTema(_shell(aoSelecionar: (i) => escolhido = i)));
      expect(find.byType(NavigationDrawer), findsOneWidget);
      expect(find.byType(NavigationRail), findsNothing);
      expect(find.byType(NavigationBar), findsNothing);
      await tester.tap(find.text('Conta'));
      expect(escolhido, 3);
    });

    testWidgets('administrador no compacto usa gaveta modal', (tester) async {
      definirTela(tester, celular);
      await tester.pumpWidget(comTema(_shell(compacta: NavegacaoCompacta.gaveta)));
      expect(find.byType(NavigationBar), findsNothing);
      await tester.tap(find.byIcon(Icons.menu));
      await tester.pumpAndSettle();
      expect(find.byType(NavigationDrawer), findsOneWidget);
    });

    testWidgets('redimensionar preserva o conteúdo e o estado do corpo', (tester) async {
      final controlador = TextEditingController();
      Widget corpo() => ShellAdaptativo(
        destinos: _destinosMorador,
        indiceSelecionado: 0,
        aoSelecionar: (_) {},
        compacta: NavegacaoCompacta.barraInferior,
        corpo: TextField(controller: controlador),
      );
      definirTela(tester, celular);
      await tester.pumpWidget(comTema(corpo()));
      await tester.enterText(find.byType(TextField), 'Lagarto');
      definirTela(tester, computador);
      await tester.pump();
      expect(find.byType(NavigationDrawer), findsOneWidget);
      expect(controlador.text, 'Lagarto');
    });

    testWidgets('360 px com texto a 200% não transborda', (tester) async {
      definirTela(tester, celular);
      await tester.pumpWidget(comTema(_shell(), escalaTexto: 2));
      expect(tester.takeException(), isNull);
      expect(find.byType(NavigationBar), findsNothing);
    });
  });

  group('estados e componentes', () {
    testWidgets('selo de situação expõe rótulo ao leitor de tela', (tester) async {
      final semantica = tester.ensureSemantics();
      await tester.pumpWidget(comTema(const Scaffold(body: SeloSituacao(situacao: SituacaoSolicitacao.agendada))));
      expect(find.bySemanticsLabel('Situação: Agendada'), findsOneWidget);
      semantica.dispose();
    });

    testWidgets('ConteudoCarga mostra carregamento, erro com nova tentativa e dados', (tester) async {
      final carga = Carga<String>();
      var tentativas = 0;
      Widget tela() => comTema(
        Scaffold(
          body: ConteudoCarga<String>(
            carga: carga,
            aoTentarNovamente: () => tentativas++,
            construir: (context, dados) => Text('dados: $dados'),
          ),
        ),
      );
      carga.carregando = true;
      await tester.pumpWidget(tela());
      expect(find.text('Carregando…'), findsOneWidget);

      carga
        ..carregando = false
        ..falha = const FalhaRede(gravacao: false);
      await tester.pumpWidget(tela());
      await tester.tap(find.text('Tentar novamente'));
      expect(tentativas, 1);

      carga
        ..falha = null
        ..dados = 'Papel';
      await tester.pumpWidget(tela());
      expect(find.text('dados: Papel'), findsOneWidget);
    });

    testWidgets('paginador habilita só as direções possíveis', (tester) async {
      int? pedida;
      const pagina = Pagina<Object?>(conteudo: [1], pagina: 0, tamanho: 20, totalElementos: 45, totalPaginas: 3);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: Paginador(pagina: pagina, aoMudar: (p) => pedida = p),
          ),
        ),
      );
      expect(find.text('Página 1 de 3'), findsOneWidget);
      await tester.tap(find.text('Anterior'));
      expect(pedida, isNull);
      await tester.tap(find.text('Próxima'));
      expect(pedida, 1);
    });

    testWidgets('componentes atendem às diretrizes de alvo e rótulo', (tester) async {
      final semantica = tester.ensureSemantics();
      definirTela(tester, celular);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: Column(
              children: [
                FilledButton(onPressed: () {}, child: const Text('Solicitar recolhimento')),
                OutlinedButton(onPressed: () {}, child: const Text('Ver detalhes')),
                IconButton(onPressed: () {}, tooltip: 'Filtros', icon: const Icon(Icons.tune)),
                const EstadoVazio(titulo: 'Nenhum ponto encontrado'),
              ],
            ),
          ),
        ),
      );
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      semantica.dispose();
    });
  });

  group('Carga', () {
    test('descarta a resposta de uma leitura superada', () async {
      final carga = Carga<String>();
      final lenta = carga.executar(() => Future.delayed(const Duration(milliseconds: 20), () => 'antiga'), () {});
      final rapida = carga.executar(() async => 'nova', () {});
      await Future.wait([lenta, rapida]);
      expect(carga.dados, 'nova');
      expect(carga.carregando, isFalse);
    });

    test('invalidar descarta resposta pendente, como na troca de ator', () async {
      final carga = Carga<String>();
      final pendente = carga.executar(() => Future.delayed(const Duration(milliseconds: 10), () => 'restrito'), () {});
      carga.invalidar();
      await pendente;
      expect(carga.dados, isNull);
    });

    test('falha preserva os dados anteriores durante a atualização', () async {
      final carga = Carga<String>()..dados = 'anterior';
      await carga.executar(() async => throw const FalhaPrazo(gravacao: false), () {});
      expect(carga.dados, 'anterior');
      expect(carga.falha, isA<FalhaPrazo>());
    });
  });
}
