import 'package:coleta_seletiva_web/app/app.dart';
import 'package:coleta_seletiva_web/app/rotas.dart';
import 'package:coleta_seletiva_web/app/sessao.dart';
import 'package:coleta_seletiva_web/core/dominio/datas.dart';
import 'package:coleta_seletiva_web/core/dominio/enums.dart';
import 'package:coleta_seletiva_web/features/resumo_gerencial/resumo_gerencial.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import 'api_falsa.dart';
import 'morador_test.dart' show dependencias, enderecoJson, moradorJson;

final resumoSeed = {
  'solicitacoesPorSituacao': [
    {'situacao': 'SOLICITADA', 'total': 3},
    {'situacao': 'AGENDADA', 'total': 1},
    {'situacao': 'EM_RECOLHIMENTO', 'total': 1},
    {'situacao': 'CONCLUIDA', 'total': 4},
    {'situacao': 'RECUSADA', 'total': 1},
    {'situacao': 'CANCELADA', 'total': 1},
  ],
  'quantidadesRecolhidas': [
    {'materialId': 6, 'nomeMaterial': 'Metal', 'unidade': 'KG', 'quantidade': 5.000},
    {'materialId': 1, 'nomeMaterial': 'Papel', 'unidade': 'KG', 'quantidade': 76.000},
    {'materialId': 2, 'nomeMaterial': 'Pilhas e baterias', 'unidade': 'UN', 'quantidade': 16.000},
    {'materialId': 4, 'nomeMaterial': 'Plástico', 'unidade': 'KG', 'quantidade': 7.250},
  ],
};

void main() {
  group('resumo gerencial', () {
    Widget tela(
      ApiFalsa api, {
      DataCivil? de,
      DataCivil? ate,
      ValueChanged<SituacaoSolicitacao>? aoAbrir,
      ResumoGerencial? reaproveitar,
    }) => comTema(
      Scaffold(
        body: PaginaResumo(
          repositorio: RepositorioResumo(api.cliente(identidade: () => {'X-Perfil': 'ADMINISTRADOR'})),
          de: de,
          ate: ate,
          reaproveitar: reaproveitar,
          aoMudarPeriodo: (_, _) {},
          aoAbrirSituacao: aoAbrir ?? (_) {},
        ),
      ),
    );

    testWidgets('1366 px: dois blocos com os valores da API e unidade por linha', (tester) async {
      final api = ApiFalsa()..quando('GET /resumo-gerencial', (_) => (200, resumoSeed));
      SituacaoSolicitacao? aberta;
      definirTela(tester, computador);
      await tester.pumpWidget(tela(api, aoAbrir: (s) => aberta = s));
      await tester.pumpAndSettle();
      expect(find.text('Solicitações criadas no período'), findsOneWidget);
      expect(find.text('Recolhido em atendimentos concluídos'), findsOneWidget);
      expect(find.text('Pela data de criação · Todo o período'), findsOneWidget);
      expect(find.text('Pela data de conclusão · Todo o período'), findsOneWidget);
      expect(find.text('76 kg'), findsOneWidget);
      expect(find.text('7,25 kg'), findsOneWidget);
      expect(find.text('16 un.'), findsOneWidget);
      expect(find.text('4'), findsOneWidget);
      await tester.tap(find.text('Concluída'));
      expect(aberta, SituacaoSolicitacao.concluida);
      expect(api.requisicoes.single.headers['X-Perfil'], 'ADMINISTRADOR');
      expect(tester.takeException(), isNull);
    });

    testWidgets('360 px: blocos empilhados, zeros e quantidades vazias', (tester) async {
      final api = ApiFalsa()
        ..quando(
          'GET /resumo-gerencial',
          (_) => (
            200,
            {
              'solicitacoesPorSituacao': [
                for (final s in SituacaoSolicitacao.conhecidas) {'situacao': s.api, 'total': 0},
              ],
              'quantidadesRecolhidas': <Object>[],
            },
          ),
        );
      definirTela(tester, celular);
      await tester.pumpWidget(tela(api, de: const DataCivil(2026, 10, 1), ate: const DataCivil(2026, 10, 31)));
      await tester.pumpAndSettle();
      expect(find.text('0'), findsNWidgets(6));
      expect(find.text('Nenhuma quantidade recolhida no período'), findsOneWidget);
      expect(find.text('Período: 01/10/2026 a 31/10/2026'), findsOneWidget);
      expect(api.requisicoes.single.url.queryParameters, {'de': '2026-10-01', 'ate': '2026-10-31'});
      expect(tester.takeException(), isNull);
    });

    testWidgets('período invertido mostra o erro do servidor em ate', (tester) async {
      final api = ApiFalsa()
        ..quando(
          'GET /resumo-gerencial',
          (_) => (
            400,
            {
              'codigo': 'VALIDACAO',
              'mensagem': 'Parâmetro inválido.',
              'erros': [
                {'campo': 'ate', 'mensagem': 'A data final não pode ser anterior à data inicial.'},
              ],
            },
          ),
        );
      await tester.pumpWidget(tela(api, de: const DataCivil(2026, 12, 1), ate: const DataCivil(2026, 11, 1)));
      await tester.pumpAndSettle();
      expect(find.text('A data final não pode ser anterior à data inicial.'), findsOneWidget);
    });

    testWidgets('reaproveita o resumo lido na validação, sem nova chamada', (tester) async {
      final api = ApiFalsa();
      await tester.pumpWidget(tela(api, reaproveitar: ResumoGerencial.deJson(resumoSeed)));
      await tester.pumpAndSettle();
      expect(find.text('76 kg'), findsOneWidget);
      expect(api.requisicoes, isEmpty);
    });
  });

  testWidgets('regressão: trocar o endereço selecionado não reinicia o início do morador', (tester) async {
    final api = ApiFalsa()
      ..quando('GET /moradores/3', (_) => (200, moradorJson(3)))
      ..quando(
        'GET /moradores/3/enderecos',
        (_) => (200, [enderecoJson(7, 3), enderecoJson(8, 3, municipio: 'Aracaju')]),
      )
      ..quando('GET /materiais', (_) => (200, pagina([materialJson(1, 'Papel')])))
      ..quando('GET /solicitacoes', (_) => (200, pagina([])));
    final d = dependencias(api)..sessao.entrar(const Ator(Perfil.morador, 3, nome: 'Maria Silva'));
    final rotas = criarRotas(d);
    definirTela(tester, computador);
    await tester.pumpWidget(AppColetaSeletiva(rotas: rotas));
    rotas.go('/morador/inicio');
    await tester.pumpAndSettle();
    expect(find.text('Pontos em Lagarto'), findsOneWidget);
    await tester.tap(find.bySemanticsLabel(RegExp('Endereço selecionado')));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Aracaju').last);
    await tester.pumpAndSettle();
    expect(find.text('Pontos em Aracaju'), findsOneWidget);
    expect(find.text('Carregando endereços…'), findsNothing);
    expect(api.chamadas('GET /moradores/3/enderecos'), hasLength(1));
  });
}
