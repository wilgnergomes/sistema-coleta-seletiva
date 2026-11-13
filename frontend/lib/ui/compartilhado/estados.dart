import 'package:material_ui/material_ui.dart';

import '../../app/tema/tema.dart';
import '../../app/tema/tokens.dart';
import '../../core/dominio/enums.dart';
import '../../core/estado/carga.dart';

/// Indicador rotulado para a primeira leitura, com área estável.
class EstadoCarregando extends StatelessWidget {
  const EstadoCarregando({super.key, this.rotulo = 'Carregando…'});

  final String rotulo;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      label: rotulo,
      excludeSemantics: true,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: Espaco.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox.square(dimension: 32, child: CircularProgressIndicator(strokeWidth: 3)),
            const SizedBox(height: Espaco.s),
            Text(rotulo, style: Theme.of(context).textTheme.bodyMedium, textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}

/// Ausência de dados, com sugestão contextual.
class EstadoVazio extends StatelessWidget {
  const EstadoVazio({super.key, required this.titulo, this.mensagem, this.icone = Icons.inbox_outlined, this.acao});

  final String titulo;
  final String? mensagem;
  final IconData icone;
  final Widget? acao;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espaco.xl, horizontal: Espaco.m),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icone, size: 48, color: Cores.textoSecundario),
          const SizedBox(height: Espaco.s),
          Text(titulo, style: texto.titleMedium, textAlign: TextAlign.center),
          if (mensagem != null) ...[
            const SizedBox(height: Espaco.xxs),
            Text(
              mensagem!,
              style: texto.bodyMedium?.copyWith(color: Cores.textoSecundario),
              textAlign: TextAlign.center,
            ),
          ],
          if (acao != null) ...[const SizedBox(height: Espaco.m), acao!],
        ],
      ),
    );
  }
}

enum TipoAviso { sucesso, alerta, erro, info }

/// Mensagem persistente com ícone e ação opcional.
class Aviso extends StatelessWidget {
  const Aviso({super.key, required this.tipo, required this.mensagem, this.titulo, this.acao});

  final TipoAviso tipo;
  final String mensagem;
  final String? titulo;
  final Widget? acao;

  @override
  Widget build(BuildContext context) {
    final (cor, fundo, icone) = switch (tipo) {
      TipoAviso.sucesso => (Cores.sucesso, Cores.sucessoFundo, Icons.check_circle_outline),
      TipoAviso.alerta => (Cores.alerta, Cores.alertaFundo, Icons.warning_amber_rounded),
      TipoAviso.erro => (Cores.erro, Cores.erroFundo, Icons.error_outline),
      TipoAviso.info => (const Color(0xFF304E67), const Color(0xFFE8F0F8), Icons.info_outline),
    };
    final texto = Theme.of(context).textTheme;
    return Semantics(
      liveRegion: true,
      container: true,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(Espaco.m),
        decoration: BoxDecoration(color: fundo, borderRadius: BorderRadius.circular(Raio.campo)),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ExcludeSemantics(child: Icon(icone, color: cor)),
            const SizedBox(width: Espaco.s),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (titulo != null) Text(titulo!, style: texto.titleSmall?.copyWith(color: cor)),
                  Text(mensagem, style: texto.bodyMedium?.copyWith(color: cor)),
                  if (acao != null) ...[const SizedBox(height: Espaco.xs), acao!],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Falha de leitura com nova tentativa.
class EstadoErro extends StatelessWidget {
  const EstadoErro({super.key, required this.mensagem, this.aoTentarNovamente});

  final String mensagem;
  final VoidCallback? aoTentarNovamente;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espaco.m),
      child: Aviso(
        tipo: TipoAviso.erro,
        mensagem: mensagem,
        acao: aoTentarNovamente == null
            ? null
            : OutlinedButton.icon(
                onPressed: aoTentarNovamente,
                icon: const Icon(Icons.refresh),
                label: const Text('Tentar novamente'),
              ),
      ),
    );
  }
}

/// Apresenta uma [Carga]: indicador na primeira leitura, erro com nova
/// tentativa, conteúdo e sinal de atualização quando já há dados.
class ConteudoCarga<T> extends StatelessWidget {
  const ConteudoCarga({
    super.key,
    required this.carga,
    required this.construir,
    this.aoTentarNovamente,
    this.rotuloCarregando = 'Carregando…',
  });

  final Carga<T> carga;
  final Widget Function(BuildContext, T) construir;
  final VoidCallback? aoTentarNovamente;
  final String rotuloCarregando;

  @override
  Widget build(BuildContext context) {
    final dados = carga.dados;
    if (dados == null) {
      if (carga.falha != null) {
        return EstadoErro(mensagem: carga.falha!.mensagem, aoTentarNovamente: aoTentarNovamente);
      }
      return Center(child: EstadoCarregando(rotulo: rotuloCarregando));
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: [
        if (carga.carregando)
          Semantics(label: 'Atualizando', child: const LinearProgressIndicator(minHeight: 2))
        else
          const SizedBox(height: 2),
        if (carga.falha != null) EstadoErro(mensagem: carga.falha!.mensagem, aoTentarNovamente: aoTentarNovamente),
        construir(context, dados),
      ],
    );
  }
}

/// Selo de situação com rótulo, ícone e cor.
class SeloSituacao extends StatelessWidget {
  const SeloSituacao({super.key, required this.situacao});

  final SituacaoSolicitacao situacao;

  @override
  Widget build(BuildContext context) =>
      _Selo(rotulo: situacao.rotulo, visual: context.coresSituacao.solicitacao(situacao));
}

class SeloCadastro extends StatelessWidget {
  const SeloCadastro({super.key, required this.situacao});

  final SituacaoCadastro situacao;

  @override
  Widget build(BuildContext context) =>
      _Selo(rotulo: situacao.rotulo, visual: context.coresSituacao.cadastro(situacao));
}

class _Selo extends StatelessWidget {
  const _Selo({required this.rotulo, required this.visual});

  final String rotulo;
  final VisualSituacao visual;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Situação: $rotulo',
      excludeSemantics: true,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: Espaco.xs, vertical: Espaco.xxs),
        decoration: BoxDecoration(color: visual.fundo, borderRadius: BorderRadius.circular(Raio.campo)),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(visual.icone, size: 16, color: visual.cor),
            const SizedBox(width: Espaco.xxs),
            Flexible(
              child: Text(rotulo, style: Theme.of(context).textTheme.labelMedium?.copyWith(color: visual.cor)),
            ),
          ],
        ),
      ),
    );
  }
}
