import 'dart:convert';

import 'package:coleta_seletiva_web/features/catalogo/data/modelos.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/repositorio_catalogo.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/admin_materiais.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/admin_pontos.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'api_falsa.dart';

Map<String, String> admin() => {'X-Perfil': 'ADMINISTRADOR'};

Finder campo(String rotulo) => find.widgetWithText(TextFormField, rotulo);

Future<void> tocar(WidgetTester tester, Finder alvo) async {
  await tester.ensureVisible(alvo);
  await tester.pumpAndSettle();
  await tester.tap(alvo);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('novo ponto: booleano explícito, endereço embutido e erro aninhado no campo', (tester) async {
    final corpos = <Map<String, Object?>>[];
    PontoColeta? criado;
    final api = ApiFalsa()
      ..quando('POST /pontos-coleta', (r) {
        final corpo = jsonDecode(r.body) as Map<String, Object?>;
        corpos.add(corpo);
        if (corpos.length == 1) {
          return (
            400,
            {
              'codigo': 'VALIDACAO',
              'mensagem': 'Há campos inválidos.',
              'erros': [
                {'campo': 'endereco.cep', 'mensagem': 'CEP recusado pelo servidor.'},
              ],
            },
          );
        }
        return (201, pontoJson(9, 'Ponto Novo', domiciliar: false));
      });
    definirTela(tester, computador);
    await tester.pumpWidget(
      comTema(
        Scaffold(
          body: PaginaEditorPonto(
            repositorio: RepositorioCatalogo(api.cliente(identidade: admin)),
            aoVoltar: () {},
            aoCriado: (p) => criado = p,
            aoVerSolicitacoes: (_) {},
          ),
        ),
      ),
    );
    await tester.enterText(campo('Nome *'), 'Ponto Novo');
    await tester.enterText(campo('Logradouro *'), 'Rua A');
    await tester.enterText(campo('Número *'), '10');
    await tester.enterText(campo('Bairro *'), 'Centro');
    await tester.enterText(campo('Município *'), 'Lagarto');
    await tester.enterText(campo('UF *'), 'SE');
    await tester.enterText(campo('CEP *'), '49400-000');
    await tocar(tester, find.text('Cadastrar ponto'));
    expect(find.text('CEP recusado pelo servidor.'), findsOneWidget);
    expect(corpos.first, {
      'nome': 'Ponto Novo',
      'contato': null,
      'horarios': null,
      'recolhimentoDomiciliar': false,
      'situacao': 'ATIVO',
      'endereco': {
        'logradouro': 'Rua A',
        'numero': '10',
        'bairro': 'Centro',
        'municipio': 'Lagarto',
        'uf': 'SE',
        'cep': '49400000',
        'complemento': null,
      },
    });
    expect(api.requisicoes.first.headers['X-Perfil'], 'ADMINISTRADOR');
    await tocar(tester, find.text('Cadastrar ponto'));
    expect(criado?.id, 9);
  });

  group('ponto existente', () {
    ApiFalsa api({required bool rn05}) => ApiFalsa()
      ..quando(
        'GET /pontos-coleta/4',
        (_) => (200, pontoJson(4, 'Cooperativa Verde Vida', materiais: [vinculoJson(1, 'Papel', limite: 100)])),
      )
      ..quando(
        'PUT /pontos-coleta/4',
        (_) => rn05
            ? (409, {'codigo': 'CONFLITO_REGRA', 'mensagem': 'O ponto possui 2 solicitações abertas.', 'erros': []})
            : (200, pontoJson(4, 'Cooperativa Verde Vida', situacao: 'INATIVO')),
      )
      ..quando('PUT /pontos-coleta/4/materiais/1', (r) => (200, vinculoJson(1, 'Papel')))
      ..quando(
        'DELETE /pontos-coleta/4/materiais/1',
        (_) => (409, {'codigo': 'CONFLITO_REGRA', 'mensagem': 'Há solicitação aberta com este material.', 'erros': []}),
      );

    Future<int?> abrir(WidgetTester tester, ApiFalsa api) async {
      int? verSolicitacoes;
      definirTela(tester, computador);
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: PaginaEditorPonto(
              id: 4,
              repositorio: RepositorioCatalogo(api.cliente(identidade: admin)),
              aoVoltar: () {},
              aoCriado: (_) {},
              aoVerSolicitacoes: (id) => verSolicitacoes = id,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      return verSolicitacoes;
    }

    testWidgets('inativação pede confirmação; RN05 mostra a mensagem e o atalho', (tester) async {
      final a = api(rn05: true);
      await abrir(tester, a);
      await tester.tap(find.text('Ativo'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Inativo').last);
      await tester.pumpAndSettle();
      await tocar(tester, find.text('Salvar ponto'));
      expect(find.text('Inativar ponto'), findsWidgets);
      await tester.tap(find.widgetWithText(FilledButton, 'Inativar ponto'));
      await tester.pumpAndSettle();
      expect(find.text('O ponto possui 2 solicitações abertas.'), findsOneWidget);
      expect(find.text('Ver solicitações do ponto'), findsOneWidget);
      final corpo = jsonDecode(a.chamadas('PUT /pontos-coleta/4').single.body) as Map<String, Object?>;
      expect(corpo['situacao'], 'INATIVO');
      expect(corpo['recolhimentoDomiciliar'], isTrue);
    });

    testWidgets('editar vínculo envia limite nulo para remover o limite; remoção em conflito orienta', (tester) async {
      final a = api(rn05: false);
      await abrir(tester, a);
      await tocar(tester, find.byTooltip('Editar vínculo de Papel'));
      expect(find.text('100'), findsOneWidget);
      await tester.enterText(campo('Limite por solicitação (kg)'), '');
      await tester.enterText(campo('Observações'), 'Somente papel seco');
      await tocar(tester, find.text('Salvar vínculo'));
      expect(jsonDecode(a.chamadas('PUT /pontos-coleta/4/materiais/1').single.body), {
        'observacoes': 'Somente papel seco',
        'limite': null,
      });
      expect(find.text('Vínculo atualizado.'), findsOneWidget);

      await tocar(tester, find.byTooltip('Remover Papel do ponto'));
      await tester.tap(find.widgetWithText(FilledButton, 'Remover material'));
      await tester.pumpAndSettle();
      expect(find.text('Há solicitação aberta com este material.'), findsOneWidget);
      expect(find.text('Ver solicitações do ponto'), findsOneWidget);
    });
  });

  testWidgets('material: troca de unidade bloqueada exibe a mensagem e preserva os dados', (tester) async {
    final api = ApiFalsa()
      ..quando('GET /materiais/1', (_) => (200, materialJson(1, 'Papel')))
      ..quando(
        'PUT /materiais/1',
        (_) => (
          409,
          {'codigo': 'CONFLITO_REGRA', 'mensagem': 'A unidade não pode mudar: há vínculos ou itens.', 'erros': []},
        ),
      );
    definirTela(tester, computador);
    await tester.pumpWidget(
      comTema(
        Scaffold(
          body: PaginaEditorMaterial(
            id: 1,
            repositorio: RepositorioCatalogo(api.cliente(identidade: admin)),
            aoVoltar: () {},
            aoSalvar: (_) {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('quilogramas (kg)'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('litros (L)').last);
    await tester.pumpAndSettle();
    await tocar(tester, find.text('Salvar material'));
    expect(find.text('A unidade não pode mudar: há vínculos ou itens.'), findsOneWidget);
    expect(find.text('litros (L)'), findsOneWidget);
    final corpo = jsonDecode(api.chamadas('PUT /materiais/1').single.body) as Map<String, Object?>;
    expect(corpo, {
      'nome': 'Papel',
      'descricao': 'Descrição de Papel',
      'unidade': 'L',
      'orientacao': 'Orientação de Papel',
      'situacao': 'ATIVO',
    });
  });
}
