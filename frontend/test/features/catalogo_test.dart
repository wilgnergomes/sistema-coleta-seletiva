import 'package:coleta_seletiva_web/core/dominio/enums.dart';
import 'package:coleta_seletiva_web/core/dominio/quantidade.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/modelos.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/repositorio_catalogo.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/pagina_detalhe_ponto.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/pagina_pontos.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'api_falsa.dart';

void main() {
  group('FiltroPontos', () {
    test('lê a URL com página a partir de 1 e ignora valores inválidos', () {
      final f = FiltroPontos.deQuery({'municipio': ' Lagarto ', 'materialId': 'x', 'pagina': '3', 'nome': ''});
      expect(f.municipio, 'Lagarto');
      expect(f.materialId, isNull);
      expect(f.nome, isNull);
      expect(f.pagina, 2);
      expect(FiltroPontos.deQuery({'pagina': '-4'}).pagina, 0);
    });

    test('ida e volta pela URL preserva os filtros', () {
      const f = FiltroPontos(nome: 'Eco', bairro: 'São José', materialId: 3, pagina: 1);
      expect(FiltroPontos.deQuery(f.paraUrl()), f);
      expect(f.paraApi(), containsPair('page', 1));
      expect(f.paraApi(), containsPair('materialId', 3));
    });
  });

  group('Quantidade', () {
    test('lê vírgula ou ponto e até 3 casas', () {
      expect(LeituraQuantidade.de('1,25', unidade: Unidade.kg).valor!.comoNumero, 1.25);
      expect(LeituraQuantidade.de('2.5', unidade: Unidade.kg).valor!.texto, '2,5');
      expect(LeituraQuantidade.de('0,001', unidade: Unidade.kg).valor!.milesimos, 1);
      expect(LeituraQuantidade.de('999999999,999', unidade: Unidade.kg).valor, Quantidade.maximo);
    });

    test('rejeita zero, excesso de dígitos, separador de milhar e fração em peças', () {
      expect(LeituraQuantidade.de('0', unidade: Unidade.kg).erro, contains('maior que zero'));
      expect(LeituraQuantidade.de('0', unidade: Unidade.kg, permitirZero: true).valida, isTrue);
      expect(LeituraQuantidade.de('1,2345', unidade: Unidade.kg).erro, contains('3 casas'));
      expect(LeituraQuantidade.de('1000000000', unidade: Unidade.kg).erro, contains('9 dígitos'));
      expect(LeituraQuantidade.de('1.234,5', unidade: Unidade.kg).valida, isFalse);
      expect(LeituraQuantidade.de('2,5', unidade: Unidade.unidade).erro, contains('inteiras'));
      expect(LeituraQuantidade.de('3,000', unidade: Unidade.unidade).valor!.comoNumero, 3);
    });

    test('formata com unidade e agrupamento de milhar', () {
      expect(Quantidade.deNumero(7.25).comUnidade(Unidade.kg), '7,25 kg');
      expect(Quantidade.deNumero(16).comUnidade(Unidade.unidade), '16 un.');
      expect(Quantidade.deNumero(1234567.5).texto, '1.234.567,5');
    });
  });

  group('PaginaPontos', () {
    late ApiFalsa api;
    late RepositorioCatalogo repositorio;
    FiltroPontos? pedido;

    setUp(() {
      pedido = null;
      api = ApiFalsa()
        ..quando(
          'GET /materiais',
          (_) => (
            200,
            pagina([
              materialJson(1, 'Papel'),
              materialJson(2, 'Plástico'),
              materialJson(3, 'Vidro'),
              materialJson(4, 'Metal'),
            ]),
          ),
        )
        ..quando(
          'GET /pontos-coleta',
          (r) => r.url.queryParameters['municipio'] == 'Nenhum'
              ? (200, pagina([]))
              : (
                  200,
                  pagina([
                    pontoJson(1, 'Cooperativa Verde Vida'),
                    pontoJson(2, 'Ecoponto Centro', domiciliar: false, municipio: 'Aracaju'),
                  ], total: 25),
                ),
        );
      repositorio = RepositorioCatalogo(api.cliente());
    });

    Widget tela(FiltroPontos filtro) => comTema(
      Scaffold(
        body: PaginaPontos(
          filtro: filtro,
          repositorio: repositorio,
          aoMudarFiltro: (f) => pedido = f,
          aoAbrirPonto: (_) {},
        ),
      ),
    );

    testWidgets('360 px: total, cartões com modalidade e paginação, sem identidade', (tester) async {
      definirTela(tester, celular);
      await tester.pumpWidget(tela(const FiltroPontos()));
      await tester.pumpAndSettle();
      expect(find.text('25 pontos de coleta'), findsOneWidget);
      expect(find.text('Cooperativa Verde Vida'), findsOneWidget);
      expect(find.text('Entrega no local'), findsOneWidget);
      expect(find.text('Página 1 de 2'), findsOneWidget);
      expect(tester.takeException(), isNull);
      final consulta = api.chamadas('GET /pontos-coleta').first;
      expect(consulta.url.queryParameters, {'page': '0', 'size': '20'});
      expect(consulta.headers.containsKey('X-Perfil'), isFalse);
    });

    testWidgets('atalho de material aplica um único materialId e volta à primeira página', (tester) async {
      definirTela(tester, computador);
      await tester.pumpWidget(tela(const FiltroPontos(municipio: 'Lagarto', pagina: 1)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Vidro'));
      expect(pedido, const FiltroPontos(municipio: 'Lagarto', materialId: 3));
    });

    testWidgets('busca por nome mantém os filtros aplicados', (tester) async {
      definirTela(tester, computador);
      await tester.pumpWidget(tela(const FiltroPontos(materialId: 1)));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).first, '  Verde  ');
      await tester.testTextInput.receiveAction(TextInputAction.search);
      expect(pedido, const FiltroPontos(nome: 'Verde', materialId: 1));
    });

    testWidgets('filtro sem resultado oferece limpar; catálogo vazio tem outra mensagem', (tester) async {
      definirTela(tester, computador);
      await tester.pumpWidget(tela(const FiltroPontos(municipio: 'Nenhum')));
      await tester.pumpAndSettle();
      expect(find.text('Nenhum ponto corresponde aos filtros'), findsOneWidget);
      await tester.tap(find.widgetWithText(OutlinedButton, 'Limpar filtros'));
      expect(pedido, const FiltroPontos());

      api.quando('GET /pontos-coleta', (_) => (200, pagina([])));
      repositorio.invalidar();
      await tester.pumpWidget(tela(const FiltroPontos()));
      await tester.pumpAndSettle();
      expect(find.text('Nenhum ponto de coleta cadastrado'), findsOneWidget);
    });

    testWidgets('falha de leitura mostra erro com nova tentativa', (tester) async {
      api.quando('GET /pontos-coleta', (_) => (500, null));
      await tester.pumpWidget(tela(const FiltroPontos()));
      await tester.pumpAndSettle();
      expect(find.text('Tentar novamente'), findsOneWidget);
      api.quando('GET /pontos-coleta', (_) => (200, pagina([pontoJson(9, 'Ponto Novo')])));
      await tester.tap(find.text('Tentar novamente'));
      await tester.pumpAndSettle();
      expect(find.text('Ponto Novo'), findsOneWidget);
    });
  });

  group('PaginaDetalhePonto', () {
    testWidgets('mostra padrões de campos ausentes, limite e material inativo', (tester) async {
      final api = ApiFalsa()
        ..quando('GET /materiais', (_) => (200, pagina([materialJson(1, 'Papel')])))
        ..quando(
          'GET /pontos-coleta/7',
          (_) => (
            200,
            pontoJson(
              7,
              'Cooperativa Verde Vida',
              materiais: [vinculoJson(1, 'Papel', limite: 12.5), vinculoJson(5, 'Isopor')],
            ),
          ),
        );
      definirTela(tester, celular);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaDetalhePonto(id: 7, repositorio: RepositorioCatalogo(api.cliente()), aoVoltar: () {}),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Contato não informado'), findsOneWidget);
      expect(find.text('Horários não informados'), findsOneWidget);
      expect(find.text('Limite por solicitação: 12,5 kg'), findsOneWidget);
      expect(find.text('· indisponível no catálogo'), findsOneWidget);
      // O catálogo de uma página é completo: nenhum GET individual de material.
      expect(api.chamadas('GET /materiais/5'), isEmpty);
      expect(tester.takeException(), isNull);
    });

    testWidgets('404 informa indisponibilidade e oferece voltar', (tester) async {
      var voltou = false;
      final api = ApiFalsa();
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaDetalhePonto(
              id: 99,
              repositorio: RepositorioCatalogo(api.cliente()),
              aoVoltar: () => voltou = true,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Ponto de coleta indisponível'), findsOneWidget);
      await tester.tap(find.text('Voltar à pesquisa'));
      expect(voltou, isTrue);
    });
  });
}
