import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';

/// Página rolável com largura máxima de leitura e margens laterais
/// adaptadas à largura disponível.
class PaginaRolavel extends StatelessWidget {
  const PaginaRolavel({super.key, required this.filhos, this.larguraMaxima = Largura.catalogo, this.controlador});

  final List<Widget> filhos;
  final double larguraMaxima;
  final ScrollController? controlador;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, restricoes) {
        final margem = margemLateral(restricoes.maxWidth);
        return Scrollbar(
          controller: controlador,
          child: SingleChildScrollView(
            controller: controlador,
            padding: EdgeInsets.fromLTRB(margem, Espaco.l, margem, Espaco.xxl),
            // Uma área de seleção para a página: protocolo, endereço e
            // orientações podem ser copiados sem que cada linha vire um alvo
            // de toque pequeno para o leitor de tela.
            child: SelectionArea(
              child: Center(
                child: ConstrainedBox(
                  constraints: BoxConstraints(maxWidth: larguraMaxima),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: filhos),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

/// Título de página com subtítulo opcional. Usa 24/32 no compacto.
class TituloPagina extends StatelessWidget {
  const TituloPagina(this.texto, {super.key, this.subtitulo, this.acao});

  final String texto;
  final String? subtitulo;
  final Widget? acao;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context).textTheme;
    final compacto = MediaQuery.sizeOf(context).width < Limiar.intermediario;
    final titulo = Semantics(
      header: true,
      child: Text(texto, style: compacto ? tema.headlineSmall : tema.headlineMedium),
    );
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaco.l),
      child: Wrap(
        alignment: WrapAlignment.spaceBetween,
        crossAxisAlignment: WrapCrossAlignment.center,
        spacing: Espaco.m,
        runSpacing: Espaco.s,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              titulo,
              if (subtitulo != null) ...[
                const SizedBox(height: Espaco.xxs),
                Text(subtitulo!, style: tema.bodyMedium?.copyWith(color: Cores.textoSecundario)),
              ],
            ],
          ),
          ?acao,
        ],
      ),
    );
  }
}

/// Título de seção.
class TituloSecao extends StatelessWidget {
  const TituloSecao(this.texto, {super.key, this.acao});

  final String texto;
  final Widget? acao;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: Espaco.l, bottom: Espaco.s),
      child: Row(
        children: [
          Expanded(
            child: Semantics(header: true, child: Text(texto, style: Theme.of(context).textTheme.titleLarge)),
          ),
          ?acao,
        ],
      ),
    );
  }
}

/// Par rótulo e valor, exposto ao leitor de tela como uma unidade.
class CampoLeitura extends StatelessWidget {
  const CampoLeitura({super.key, required this.rotulo, required this.valor});

  final String rotulo;
  final String valor;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context).textTheme;
    return MergeSemantics(
      child: Padding(
        padding: const EdgeInsets.only(bottom: Espaco.s),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(rotulo, style: tema.bodySmall),
            const SizedBox(height: 2),
            Text(valor, style: tema.bodyLarge),
          ],
        ),
      ),
    );
  }
}

/// Grade simples que distribui filhos em colunas de largura mínima.
class GradeResponsiva extends StatelessWidget {
  const GradeResponsiva({super.key, required this.filhos, this.larguraMinima = 248, this.espaco = Espaco.m});

  final List<Widget> filhos;
  final double larguraMinima;
  final double espaco;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, restricoes) {
        final colunas = ((restricoes.maxWidth + espaco) / (larguraMinima + espaco)).floor().clamp(1, 4);
        final largura = (restricoes.maxWidth - espaco * (colunas - 1)) / colunas;
        return Wrap(
          spacing: espaco,
          runSpacing: espaco,
          children: [for (final f in filhos) SizedBox(width: largura, child: f)],
        );
      },
    );
  }
}
