import 'dart:convert';

import 'package:coleta_seletiva_web/app/app.dart';
import 'package:coleta_seletiva_web/app/configuracao.dart';
import 'package:coleta_seletiva_web/app/dependencias.dart';
import 'package:coleta_seletiva_web/app/rotas.dart';
import 'package:coleta_seletiva_web/app/sessao.dart';
import 'package:coleta_seletiva_web/core/api/falhas.dart';
import 'package:coleta_seletiva_web/features/demonstracao/pagina_demonstracao.dart';
import 'package:coleta_seletiva_web/features/moradores/data/moradores.dart';
import 'package:coleta_seletiva_web/features/moradores/ui/formulario_endereco.dart';
import 'package:coleta_seletiva_web/features/moradores/ui/paginas_morador.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'api_falsa.dart';

Map<String, Object?> moradorJson(int id, {String situacao = 'ATIVO'}) => {
  'id': id,
  'nome': 'Maria Silva',
  'email': 'maria.silva@exemplo.com',
  'fone': null,
  'situacao': situacao,
};

Map<String, Object?> enderecoJson(int id, int moradorId, {String municipio = 'Lagarto', String numero = '120'}) => {
  'id': id,
  'moradorId': moradorId,
  'logradouro': 'Rua das Flores',
  'numero': numero,
  'bairro': 'Centro',
  'municipio': municipio,
  'uf': 'SE',
  'cep': '49400000',
  'complemento': null,
};

Dependencias dependencias(ApiFalsa api, {String modo = 'demo'}) {
  final sessao = Sessao();
  return Dependencias(
    configuracao: ConfiguracaoApp.deTextos(modo: modo, apiBaseUrl: '/api', fusoNegocio: 'America/Sao_Paulo'),
    api: api.cliente(identidade: sessao.cabecalhos),
    sessao: sessao,
  );
}

void main() {
  group('Sessao e perfis', () {
    test('cabeçalhos por perfil: administrador dispensa o identificador', () {
      expect(const Ator(Perfil.morador, 3).cabecalhos, {'X-Perfil': 'MORADOR', 'X-Usuario-Id': '3'});
      expect(const Ator(Perfil.administrador, null).cabecalhos, {'X-Perfil': 'ADMINISTRADOR'});
    });

    test('trocar de ator limpa endereço selecionado e chama as invalidações', () {
      final sessao = Sessao();
      var invalidacoes = 0;
      sessao.aoTrocarAtor(() => invalidacoes++);
      sessao.entrar(const Ator(Perfil.morador, 3), respostaValidacao: 'x');
      sessao.enderecoSelecionadoId = 9;
      final geracao = sessao.geracao;
      sessao.entrar(const Ator(Perfil.operador, 1));
      expect(sessao.enderecoSelecionadoId, isNull);
      expect(sessao.consumirRespostaValidacao(), isNull);
      expect(sessao.geracao, greaterThan(geracao));
      expect(invalidacoes, 2);
      sessao.sair();
      expect(sessao.cabecalhos(), isEmpty);
    });

    test('rota identifica a área do perfil', () {
      expect(Perfil.daRota('/morador/solicitacoes/2'), Perfil.morador);
      expect(Perfil.daRota('/administracao'), Perfil.administrador);
      expect(Perfil.daRota('/moradores'), isNull);
      expect(Perfil.daRota('/pontos'), isNull);
    });

    test('retorno após a seleção só aceita rota interna do mesmo perfil', () {
      const morador = Ator(Perfil.morador, 3);
      expect(destinoAposSelecao(morador, '/morador/solicitacoes/4'), '/morador/solicitacoes/4');
      expect(destinoAposSelecao(morador, '/operador/fila'), '/morador/inicio');
      expect(destinoAposSelecao(morador, '//evil.example/morador'), '/morador/inicio');
      expect(destinoAposSelecao(morador, 'https://evil.example'), '/morador/inicio');
      expect(destinoAposSelecao(morador, null), '/morador/inicio');
    });
  });

  group('guard de rotas', () {
    late ApiFalsa api;

    setUp(() {
      api = ApiFalsa()
        ..quando('GET /pontos-coleta', (_) => (200, pagina([])))
        ..quando('GET /materiais', (_) => (200, pagina([])));
    });

    Future<GoRouter> abrir(WidgetTester tester, Dependencias d, String rota) async {
      final rotas = criarRotas(d);
      await tester.pumpWidget(AppColetaSeletiva(rotas: rotas));
      rotas.go(rota);
      await tester.pumpAndSettle();
      return rotas;
    }

    testWidgets('rota restrita sem ator leva à demonstração com retorno', (tester) async {
      final rotas = await abrir(tester, dependencias(api), '/morador/conta');
      expect(rotas.state.uri.path, '/demonstracao');
      expect(rotas.state.uri.queryParameters['retorno'], '/morador/conta');
      expect(find.text('Identidade de demonstração'), findsOneWidget);
    });

    testWidgets('perfil diferente recebe orientação, sem acessar a área', (tester) async {
      final d = dependencias(api)..sessao.entrar(const Ator(Perfil.operador, 1));
      final rotas = await abrir(tester, d, '/morador/conta');
      expect(rotas.state.uri.path, '/acesso-restrito');
      expect(find.text('Área de outro perfil'), findsOneWidget);
      expect(api.chamadas('GET /moradores/1'), isEmpty);
    });

    testWidgets('compilação pública não oferece demonstração nem áreas restritas', (tester) async {
      final rotas = await abrir(tester, dependencias(api, modo: 'publico'), '/demonstracao');
      expect(rotas.state.uri.path, '/pontos');
      rotas.go('/morador/inicio');
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/pontos');
      expect(find.text('Demonstração'), findsNothing);
    });

    testWidgets('morador entra pela demonstração e volta à rota pedida com cabeçalhos', (tester) async {
      api
        ..quando('GET /moradores/3', (_) => (200, moradorJson(3)))
        ..quando('GET /moradores/3/enderecos', (_) => (200, [enderecoJson(7, 3)]));
      definirTela(tester, computador);
      final d = dependencias(api);
      final rotas = await abrir(tester, d, '/morador/conta');
      await tester.enterText(find.byType(TextFormField), '3');
      await tester.ensureVisible(find.text('Usar este ator'));
      await tester.tap(find.text('Usar este ator'));
      await tester.pumpAndSettle();
      expect(rotas.state.uri.path, '/morador/conta');
      expect(d.sessao.ator?.nome, 'Maria Silva');
      expect(find.textContaining('Simulação de identidade · Morador #3'), findsOneWidget);
      final validacao = api.chamadas('GET /moradores/3').first;
      expect(validacao.headers['X-Perfil'], 'MORADOR');
      expect(validacao.headers['X-Usuario-Id'], '3');
    });
  });

  group('ValidadorAtor', () {
    test('morador inativo não é selecionado', () async {
      final api = ApiFalsa()..quando('GET /moradores/1', (_) => (200, moradorJson(1, situacao: 'INATIVO')));
      final cliente = api.cliente();
      final validador = ValidadorAtor(cliente, RepositorioMoradores(cliente));
      expect(validador.validar(Perfil.morador, 1), throwsA(isA<ErroHttp>()));
    });

    test('operador é validado pela primeira página da fila, inclusive vazia', () async {
      final api = ApiFalsa()..quando('GET /solicitacoes', (_) => (200, pagina([])));
      final cliente = api.cliente();
      final (ator, resposta) = await ValidadorAtor(cliente, RepositorioMoradores(cliente)).validar(Perfil.operador, 2);
      expect(ator.perfil, Perfil.operador);
      expect(resposta, isA<Map<String, Object?>>());
      final chamada = api.requisicoes.single;
      expect(chamada.url.queryParameters, {'situacao': 'SOLICITADA', 'page': '0', 'size': '20'});
      expect(chamada.headers['X-Usuario-Id'], '2');
    });
  });

  group('cadastro público', () {
    testWidgets('e-mail duplicado preserva o formulário; sucesso oferece a demonstração', (tester) async {
      var respostaConflito = true;
      final api = ApiFalsa()
        ..quando(
          'POST /moradores',
          (r) => respostaConflito
              ? (409, {'codigo': 'CONFLITO_REGRA', 'mensagem': 'Já existe um morador com este e-mail.', 'erros': []})
              : (201, moradorJson(12)),
        );
      Morador? usado;
      definirTela(tester, celular);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaCadastro(
              repositorio: RepositorioMoradores(api.cliente()),
              demonstracao: true,
              aoUsarNaDemonstracao: (m) => usado = m,
              aoVoltarAoCatalogo: () {},
            ),
          ),
        ),
      );
      expect(find.text('Situação *'), findsNothing);
      final campos = find.byType(TextFormField);
      await tester.enterText(campos.at(0), 'Maria Silva');
      await tester.enterText(campos.at(1), 'maria.silva@exemplo.com');
      await tester.ensureVisible(find.text('Cadastrar morador').last);
      await tester.tap(find.text('Cadastrar morador').last);
      await tester.pumpAndSettle();
      expect(find.text('Já existe um morador com este e-mail.'), findsOneWidget);
      expect(find.text('maria.silva@exemplo.com'), findsOneWidget);

      final corpo = jsonDecode(api.requisicoes.single.body) as Map<String, Object?>;
      expect(corpo, {'nome': 'Maria Silva', 'email': 'maria.silva@exemplo.com', 'fone': null});
      expect(api.requisicoes.single.headers.containsKey('X-Perfil'), isFalse);

      respostaConflito = false;
      await tester.tap(find.text('Cadastrar morador').last);
      await tester.pumpAndSettle();
      expect(find.text('Cadastro concluído'), findsOneWidget);
      await tester.tap(find.text('Usar este cadastro na demonstração'));
      expect(usado?.id, 12);
    });

    testWidgets('validação local aponta obrigatórios e e-mail inválido sem enviar', (tester) async {
      final api = ApiFalsa();
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaCadastro(
              repositorio: RepositorioMoradores(api.cliente()),
              demonstracao: false,
              aoUsarNaDemonstracao: (_) {},
              aoVoltarAoCatalogo: () {},
            ),
          ),
        ),
      );
      await tester.enterText(find.byType(TextFormField).at(1), 'sem-arroba');
      await tester.tap(find.text('Cadastrar morador').last);
      await tester.pumpAndSettle();
      expect(find.text('Informe nome.'), findsOneWidget);
      expect(find.text('Informe um e-mail válido.'), findsOneWidget);
      expect(api.requisicoes, isEmpty);
    });
  });

  group('formulário de endereço', () {
    testWidgets('S/N, UF maiúscula e CEP só com dígitos; erro do servidor no campo', (tester) async {
      Map<String, Object?>? enviado;
      final api = ApiFalsa()
        ..quando('POST /moradores/3/enderecos', (r) {
          enviado = jsonDecode(r.body) as Map<String, Object?>;
          return (
            400,
            {
              'codigo': 'VALIDACAO',
              'mensagem': 'Há campos inválidos.',
              'erros': [
                {'campo': 'bairro', 'mensagem': 'Bairro recusado pelo servidor.'},
              ],
            },
          );
        });
      final repositorio = RepositorioMoradores(api.cliente());
      definirTela(tester, celular);
      await tester.pumpWidget(
        comTema(Scaffold(body: FormularioEndereco(salvar: (e) => repositorio.criarEndereco(3, e)))),
      );
      final campos = find.byType(TextFormField);
      await tester.enterText(campos.at(0), 'Avenida Principal');
      await tester.tap(find.text('Sem número (S/N)'));
      await tester.pump();
      await tester.enterText(campos.at(3), 'Zona Rural');
      await tester.enterText(campos.at(4), 'Lagarto');
      await tester.enterText(campos.at(5), 'se');
      await tester.enterText(campos.at(6), '49400-001');
      await tester.ensureVisible(find.text('Salvar endereço'));
      await tester.tap(find.text('Salvar endereço'));
      await tester.pumpAndSettle();
      expect(enviado, {
        'logradouro': 'Avenida Principal',
        'numero': 'S/N',
        'bairro': 'Zona Rural',
        'municipio': 'Lagarto',
        'uf': 'SE',
        'cep': '49400001',
        'complemento': null,
      });
      expect(find.text('Bairro recusado pelo servidor.'), findsOneWidget);
      expect(find.text('Zona Rural'), findsOneWidget);
    });

    testWidgets('CEP incompleto é recusado antes do envio', (tester) async {
      final api = ApiFalsa();
      await tester.pumpWidget(
        comTema(
          Scaffold(body: FormularioEndereco(salvar: (e) => RepositorioMoradores(api.cliente()).criarEndereco(1, e))),
        ),
      );
      await tester.enterText(find.byType(TextFormField).at(6), '4940');
      await tester.ensureVisible(find.text('Salvar endereço'));
      await tester.tap(find.text('Salvar endereço'));
      await tester.pumpAndSettle();
      expect(find.text('O CEP deve ter 8 dígitos.'), findsOneWidget);
      expect(api.requisicoes, isEmpty);
    });
  });
}
