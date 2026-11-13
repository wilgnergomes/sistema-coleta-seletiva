import 'dart:convert';

import 'package:coleta_seletiva_web/features/catalogo/data/repositorio_catalogo.dart';
import 'package:coleta_seletiva_web/features/moradores/data/moradores.dart';
import 'package:coleta_seletiva_web/features/moradores/ui/admin_moradores.dart';
import 'package:coleta_seletiva_web/features/operadores/operadores.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'admin_catalogo_test.dart' show admin, campo, tocar;
import 'api_falsa.dart';
import 'morador_test.dart' show enderecoJson, moradorJson;

Map<String, Object?> operadorJson(int id, {int pontoId = 4, String situacao = 'ATIVO'}) => {
  'id': id,
  'nome': 'Carla Nunes',
  'email': 'carla@exemplo.com',
  'pontoId': pontoId,
  'situacao': situacao,
};

ApiFalsa apiPessoas() => ApiFalsa()
  ..quando(
    'GET /pontos-coleta',
    (r) => (
      200,
      pagina([
        pontoJson(4, 'Cooperativa Verde Vida'),
        pontoJson(1, 'Ecoponto Desativado', situacao: 'INATIVO'),
      ], tamanho: 100),
    ),
  );

void main() {
  group('operadores', () {
    testWidgets('lista filtra por ponto e mostra o nome do ponto', (tester) async {
      final api = apiPessoas()..quando('GET /operadores', (_) => (200, pagina([operadorJson(1)])));
      final cliente = api.cliente(identidade: admin);
      definirTela(tester, celular);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaOperadores(
              repositorio: RepositorioOperadores(cliente),
              catalogo: RepositorioCatalogo(cliente),
              pontoId: 4,
              pagina: 0,
              aoMudar: (_, _) {},
              aoAbrir: (_) async {},
              aoNovo: () async {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(api.chamadas('GET /operadores').single.url.queryParameters, {'pontoId': '4', 'page': '0', 'size': '20'});
      expect(find.text('Cooperativa Verde Vida'), findsOneWidget);
      expect(find.text('Ponto: Cooperativa Verde Vida'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('mudar o vínculo de ponto pede confirmação; e-mail duplicado aparece', (tester) async {
      final api = apiPessoas()
        ..quando('GET /operadores/1', (_) => (200, operadorJson(1)))
        ..quando(
          'PUT /operadores/1',
          (_) => (409, {'codigo': 'CONFLITO_REGRA', 'mensagem': 'Já existe um operador com este e-mail.', 'erros': []}),
        );
      final cliente = api.cliente(identidade: admin);
      definirTela(tester, computador);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaEditorOperador(
              id: 1,
              repositorio: RepositorioOperadores(cliente),
              catalogo: RepositorioCatalogo(cliente),
              aoVoltar: () {},
              aoSalvar: (_) {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Cooperativa Verde Vida'), findsOneWidget);
      await tocar(tester, find.text('Escolher ponto'));
      await tocar(tester, find.text('Ecoponto Desativado'));
      expect(find.text('Ecoponto Desativado (inativo)'), findsOneWidget);
      await tocar(tester, find.text('Salvar operador').last);
      expect(find.textContaining('a fila passará a ser a de Ecoponto Desativado (inativo)'), findsOneWidget);
      await tester.tap(find.widgetWithText(FilledButton, 'Salvar operador').last);
      await tester.pumpAndSettle();
      expect(jsonDecode(api.chamadas('PUT /operadores/1').single.body), {
        'nome': 'Carla Nunes',
        'email': 'carla@exemplo.com',
        'pontoId': 1,
        'situacao': 'ATIVO',
      });
      expect(find.text('Já existe um operador com este e-mail.'), findsOneWidget);
    });

    testWidgets('novo operador exige ponto', (tester) async {
      final api = apiPessoas();
      final cliente = api.cliente(identidade: admin);
      definirTela(tester, computador);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaEditorOperador(
              repositorio: RepositorioOperadores(cliente),
              catalogo: RepositorioCatalogo(cliente),
              aoVoltar: () {},
              aoSalvar: (_) {},
            ),
          ),
        ),
      );
      await tester.enterText(campo('Nome *'), 'Paulo Santos');
      await tester.enterText(campo('E-mail *'), 'paulo@exemplo.com');
      await tocar(tester, find.text('Salvar operador'));
      expect(find.text('Escolha o ponto do operador.'), findsOneWidget);
      expect(api.chamadas('POST /operadores'), isEmpty);
    });
  });

  testWidgets('morador administrativo: situação, inativação confirmada e endereços', (tester) async {
    Map<String, Object?>? corpo;
    final api = ApiFalsa()
      ..quando('GET /moradores/3', (_) => (200, moradorJson(3)))
      ..quando('GET /moradores/3/enderecos', (_) => (200, [enderecoJson(7, 3)]))
      ..quando('PUT /moradores/3', (r) {
        corpo = jsonDecode(r.body) as Map<String, Object?>;
        return (200, moradorJson(3, situacao: 'INATIVO'));
      });
    definirTela(tester, computador);
    await tester.pumpWidget(
      comTema(
        Scaffold(
          body: PaginaMoradorAdmin(
            id: 3,
            repositorio: RepositorioMoradores(api.cliente(identidade: admin)),
            aoVoltar: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Rua das Flores, 120'), findsOneWidget);
    await tocar(tester, find.text('Ativo'));
    await tocar(tester, find.text('Inativo').last);
    await tocar(tester, find.text('Salvar morador'));
    expect(find.textContaining('não poderá criar novas solicitações'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Inativar morador'));
    await tester.pumpAndSettle();
    expect(corpo, {'nome': 'Maria Silva', 'email': 'maria.silva@exemplo.com', 'fone': null, 'situacao': 'INATIVO'});
    expect(find.text('Morador atualizado.'), findsOneWidget);
  });
}
