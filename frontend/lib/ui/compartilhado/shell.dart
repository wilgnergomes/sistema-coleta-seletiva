import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';

class DestinoNavegacao {
  const DestinoNavegacao({
    required this.rotulo,
    required this.icone,
    required this.iconeSelecionado,
    required this.rota,
  });

  final String rotulo;
  final IconData icone;
  final IconData iconeSelecionado;
  final String rota;
}

/// Navegação usada abaixo de 600 dp de largura útil.
enum NavegacaoCompacta {
  /// Barra inferior (morador).
  barraInferior,

  /// Gaveta modal (administrador).
  gaveta,

  /// Só o cabeçalho (público e operador).
  cabecalho,
}

/// Forma de navegação calculada a partir da largura e da escala de texto.
enum FormaNavegacao { barraInferior, gaveta, cabecalho, trilho, lateral }

FormaNavegacao formaNavegacao({
  required double largura,
  required int quantidadeDestinos,
  required NavegacaoCompacta compacta,
  double escalaTexto = 1,
}) {
  // Com texto muito ampliado, os rótulos laterais não cabem: usa a gaveta.
  if (quantidadeDestinos >= 2 && largura >= Limiar.amplo && escalaTexto > 1.5) return FormaNavegacao.gaveta;
  if (quantidadeDestinos >= 2 && largura >= Limiar.amplo) return FormaNavegacao.lateral;
  if (quantidadeDestinos >= 2 && largura >= Limiar.intermediario) return FormaNavegacao.trilho;
  return switch (compacta) {
    // Com texto muito ampliado, os rótulos da barra perdem legibilidade.
    NavegacaoCompacta.barraInferior when escalaTexto > 1.5 => FormaNavegacao.gaveta,
    NavegacaoCompacta.barraInferior => FormaNavegacao.barraInferior,
    NavegacaoCompacta.gaveta => FormaNavegacao.gaveta,
    NavegacaoCompacta.cabecalho => FormaNavegacao.cabecalho,
  };
}

/// Estrutura comum das áreas: cabeçalho, navegação adaptativa e conteúdo.
///
/// As variantes de layout recebem o mesmo [corpo], de modo que filtros,
/// seleção e rascunhos, mantidos nos ViewModels, sobrevivem ao
/// redimensionamento.
class ShellAdaptativo extends StatelessWidget {
  const ShellAdaptativo({
    super.key,
    required this.destinos,
    required this.indiceSelecionado,
    required this.aoSelecionar,
    required this.corpo,
    this.compacta = NavegacaoCompacta.cabecalho,
    this.acoes = const [],
    this.titulo,
    this.faixa,
  });

  final List<DestinoNavegacao> destinos;

  /// Índice do destino atual, ou nulo quando a rota não pertence a nenhum.
  final int? indiceSelecionado;
  final ValueChanged<int> aoSelecionar;
  final Widget corpo;
  final NavegacaoCompacta compacta;
  final List<Widget> acoes;
  final Widget? titulo;

  /// Faixa acima do conteúdo, como o aviso de simulação de identidade.
  final Widget? faixa;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, restricoes) {
        final forma = formaNavegacao(
          largura: restricoes.maxWidth,
          quantidadeDestinos: destinos.length,
          compacta: compacta,
          escalaTexto: MediaQuery.textScalerOf(context).scale(16) / 16,
        );
        final conteudo = Column(
          children: [
            ?faixa,
            Expanded(child: corpo),
          ],
        );
        final cabecalho = AppBar(
          title: titulo,
          actions: [
            ...acoes,
            const SizedBox(width: Espaco.xs),
          ],
          titleSpacing: Espaco.m,
        );

        return switch (forma) {
          FormaNavegacao.barraInferior => Scaffold(
            appBar: cabecalho,
            body: conteudo,
            bottomNavigationBar: NavigationBar(
              selectedIndex: indiceSelecionado ?? 0,
              indicatorColor: indiceSelecionado == null ? Colors.transparent : null,
              onDestinationSelected: aoSelecionar,
              destinations: [
                for (final d in destinos)
                  NavigationDestination(icon: Icon(d.icone), selectedIcon: Icon(d.iconeSelecionado), label: d.rotulo),
              ],
            ),
          ),
          FormaNavegacao.gaveta => Scaffold(
            appBar: cabecalho,
            drawer: NavigationDrawer(
              selectedIndex: indiceSelecionado,
              onDestinationSelected: (i) {
                Navigator.of(context).pop();
                aoSelecionar(i);
              },
              children: [
                const SizedBox(height: Espaco.m),
                for (final d in destinos)
                  NavigationDrawerDestination(
                    icon: Icon(d.icone),
                    selectedIcon: Icon(d.iconeSelecionado),
                    label: Text(d.rotulo),
                  ),
              ],
            ),
            body: conteudo,
          ),
          FormaNavegacao.cabecalho => Scaffold(appBar: cabecalho, body: conteudo),
          FormaNavegacao.trilho => Scaffold(
            appBar: cabecalho,
            body: Row(
              children: [
                NavigationRail(
                  labelType: NavigationRailLabelType.all,
                  selectedIndex: indiceSelecionado,
                  onDestinationSelected: aoSelecionar,
                  destinations: [
                    for (final d in destinos)
                      NavigationRailDestination(
                        icon: Icon(d.icone),
                        selectedIcon: Icon(d.iconeSelecionado),
                        label: Text(d.rotulo),
                      ),
                  ],
                ),
                const VerticalDivider(width: 1),
                Expanded(child: conteudo),
              ],
            ),
          ),
          // Lateral persistente: gaveta fixa de 232 dp, com destinos de 56 dp
          // (o trilho estendido teria destinos de 44 dp, abaixo do alvo mínimo).
          FormaNavegacao.lateral => Scaffold(
            appBar: cabecalho,
            body: Row(
              children: [
                SizedBox(
                  width: 232,
                  child: NavigationDrawer(
                    elevation: 0,
                    selectedIndex: indiceSelecionado,
                    onDestinationSelected: aoSelecionar,
                    tilePadding: const EdgeInsets.symmetric(horizontal: Espaco.s),
                    children: [
                      const SizedBox(height: Espaco.s),
                      for (final d in destinos)
                        NavigationDrawerDestination(
                          icon: Icon(d.icone),
                          selectedIcon: Icon(d.iconeSelecionado),
                          label: SizedBox(
                            width: 140,
                            child: Text(d.rotulo, maxLines: 1, overflow: TextOverflow.ellipsis),
                          ),
                        ),
                    ],
                  ),
                ),
                const VerticalDivider(width: 1),
                Expanded(child: conteudo),
              ],
            ),
          ),
        };
      },
    );
  }
}
