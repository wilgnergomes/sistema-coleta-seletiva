import 'package:material_ui/material_ui.dart';

import '../../app/tema/tema.dart';
import '../../app/tema/tokens.dart';
import '../../core/api/cliente_api.dart';
import '../../core/api/falhas.dart';
import '../../core/api/json.dart';
import '../../core/dominio/datas.dart';
import '../../core/dominio/enums.dart';
import '../../core/dominio/quantidade.dart';
import '../../core/estado/carga.dart';
import '../../ui/compartilhado/estados.dart';
import '../../ui/compartilhado/layout.dart';
import '../../ui/compartilhado/material_visual.dart';
import '../solicitacoes/ui/componentes_solicitacao.dart';

class TotalSituacao {
  const TotalSituacao(this.situacao, this.total);

  final SituacaoSolicitacao situacao;
  final int total;
}

class QuantidadeRecolhida {
  const QuantidadeRecolhida({
    required this.materialId,
    required this.nome,
    required this.unidade,
    required this.quantidade,
  });

  final int materialId;
  final String nome;
  final Unidade unidade;
  final Quantidade quantidade;
}

/// RF15: contagem por situação pela data de criação e quantidades efetivas
/// das concluídas pela data de conclusão. Os valores são exibidos como a
/// API os entrega; nada é somado no cliente.
class ResumoGerencial {
  const ResumoGerencial({required this.porSituacao, required this.quantidades});

  factory ResumoGerencial.deJson(Object? json) {
    final mapa = comoMapa(json);
    return ResumoGerencial(
      porSituacao: mapa.lista(
        'solicitacoesPorSituacao',
        (j) => TotalSituacao(SituacaoSolicitacao.deApi(j.textoOpcional('situacao')), j.inteiro('total')),
      ),
      quantidades: mapa.lista(
        'quantidadesRecolhidas',
        (j) => QuantidadeRecolhida(
          materialId: j.inteiro('materialId'),
          nome: j.texto('nomeMaterial'),
          unidade: Unidade.deApi(j.textoOpcional('unidade')),
          quantidade: Quantidade.deNumero(j.numero('quantidade')),
        ),
      ),
    );
  }

  final List<TotalSituacao> porSituacao;
  final List<QuantidadeRecolhida> quantidades;
}

class RepositorioResumo {
  RepositorioResumo(this._api);

  final ClienteApi _api;

  Future<ResumoGerencial> obter({DataCivil? de, DataCivil? ate}) async => ResumoGerencial.deJson(
    await _api.get('/resumo-gerencial', query: {'de': de?.iso, 'ate': ate?.iso}, acesso: Acesso.ator),
  );
}

/// `/administracao/resumo`. O período vive na URL e atualiza os dois blocos.
class PaginaResumo extends StatefulWidget {
  const PaginaResumo({
    super.key,
    required this.repositorio,
    required this.de,
    required this.ate,
    required this.aoMudarPeriodo,
    required this.aoAbrirSituacao,
    this.reaproveitar,
  });

  final RepositorioResumo repositorio;
  final DataCivil? de;
  final DataCivil? ate;
  final void Function(DataCivil? de, DataCivil? ate) aoMudarPeriodo;

  /// Abre a consulta RF09 com a situação e o mesmo período de criação.
  final ValueChanged<SituacaoSolicitacao> aoAbrirSituacao;

  /// Resumo sem período lido na validação do administrador.
  final ResumoGerencial? reaproveitar;

  @override
  State<PaginaResumo> createState() => _PaginaResumoState();
}

class _PaginaResumoState extends State<PaginaResumo> {
  final _carga = Carga<ResumoGerencial>();

  void _carregar() {
    final reaproveitado = widget.reaproveitar;
    if (reaproveitado != null && widget.de == null && widget.ate == null && _carga.dados == null) {
      _carga.definir(reaproveitado);
      return;
    }
    _carga.executar(() => widget.repositorio.obter(de: widget.de, ate: widget.ate), () {
      if (mounted) setState(() {});
    });
  }

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void didUpdateWidget(PaginaResumo antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.de != widget.de || antigo.ate != widget.ate) _carregar();
  }

  Future<void> _periodo() async {
    final r = await escolherPeriodo(context, de: widget.de, ate: widget.ate, titulo: 'Período do resumo');
    if (r != null) widget.aoMudarPeriodo(r.$1, r.$2);
  }

  @override
  Widget build(BuildContext context) {
    final periodo = rotuloPeriodo(widget.de, widget.ate);
    final falha = _carga.falha;
    final erroPeriodo = falha is ErroHttp ? falha.errosPorCampo['ate'] ?? falha.errosPorCampo['de'] : null;
    return PaginaRolavel(
      larguraMaxima: Largura.densa,
      filhos: [
        const TituloPagina('Resumo gerencial', subtitulo: 'Indicadores oficiais do RF15, no fuso de negócio.'),
        Wrap(
          spacing: Espaco.s,
          runSpacing: Espaco.s,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Semantics(
              liveRegion: true,
              child: Text('Período: $periodo', style: Theme.of(context).textTheme.titleMedium),
            ),
            FilledButton.icon(
              onPressed: _periodo,
              icon: const Icon(Icons.date_range_outlined),
              label: const Text('Alterar período'),
            ),
            if (widget.de != null || widget.ate != null)
              TextButton(onPressed: () => widget.aoMudarPeriodo(null, null), child: const Text('Limpar período')),
          ],
        ),
        if (erroPeriodo != null) ...[
          const SizedBox(height: Espaco.s),
          Aviso(tipo: TipoAviso.erro, mensagem: erroPeriodo),
        ],
        const SizedBox(height: Espaco.l),
        ConteudoCarga<ResumoGerencial>(
          carga: _carga,
          rotuloCarregando: 'Carregando resumo…',
          aoTentarNovamente: _carregar,
          construir: (context, resumo) => LayoutBuilder(
            builder: (context, restricoes) {
              final escala = MediaQuery.textScalerOf(context).scale(16) / 16;
              final lado = restricoes.maxWidth >= 744 && escala <= 1.3;
              final contagem = _BlocoContagem(
                resumo: resumo,
                periodo: periodo,
                aoAbrirSituacao: widget.aoAbrirSituacao,
              );
              final quantidades = _BlocoQuantidades(resumo: resumo, periodo: periodo);
              if (!lado) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    contagem,
                    const SizedBox(height: Espaco.l),
                    quantidades,
                  ],
                );
              }
              return Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(child: contagem),
                  const SizedBox(width: Espaco.l),
                  Expanded(child: quantidades),
                ],
              );
            },
          ),
        ),
      ],
    );
  }
}

class _BlocoContagem extends StatelessWidget {
  const _BlocoContagem({required this.resumo, required this.periodo, required this.aoAbrirSituacao});

  final ResumoGerencial resumo;
  final String periodo;
  final ValueChanged<SituacaoSolicitacao> aoAbrirSituacao;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final maior = resumo.porSituacao.fold<int>(0, (m, t) => t.total > m ? t.total : m);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(Espaco.m),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Semantics(header: true, child: Text('Solicitações criadas no período', style: texto.titleLarge)),
            Text('Pela data de criação · $periodo', style: texto.bodySmall),
            const SizedBox(height: Espaco.m),
            for (final t in resumo.porSituacao)
              _LinhaContagem(total: t, maior: maior, aoAbrir: () => aoAbrirSituacao(t.situacao)),
          ],
        ),
      ),
    );
  }
}

class _LinhaContagem extends StatelessWidget {
  const _LinhaContagem({required this.total, required this.maior, required this.aoAbrir});

  final TotalSituacao total;
  final int maior;
  final VoidCallback aoAbrir;

  @override
  Widget build(BuildContext context) {
    final visual = context.coresSituacao.solicitacao(total.situacao);
    final proporcao = maior == 0 ? 0.0 : total.total / maior;
    final texto = Theme.of(context).textTheme;
    return Semantics(
      button: true,
      label: '${total.situacao.rotulo}: ${total.total}. Abrir solicitações desta situação.',
      excludeSemantics: true,
      child: InkWell(
        onTap: aoAbrir,
        borderRadius: BorderRadius.circular(Raio.campo),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: Espaco.xs, horizontal: Espaco.xxs),
          child: Row(
            children: [
              Icon(visual.icone, size: 20, color: visual.cor),
              const SizedBox(width: Espaco.xs),
              SizedBox(width: 132, child: Text(total.situacao.rotulo, style: texto.bodyMedium)),
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(Raio.pilula),
                  child: LinearProgressIndicator(
                    value: proporcao,
                    minHeight: 10,
                    color: visual.cor,
                    backgroundColor: Cores.neutroFundo,
                  ),
                ),
              ),
              const SizedBox(width: Espaco.s),
              SizedBox(
                width: 48,
                child: Text('${total.total}', style: texto.titleMedium, textAlign: TextAlign.end),
              ),
              const Icon(Icons.chevron_right, size: 20),
            ],
          ),
        ),
      ),
    );
  }
}

class _BlocoQuantidades extends StatelessWidget {
  const _BlocoQuantidades({required this.resumo, required this.periodo});

  final ResumoGerencial resumo;
  final String periodo;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(Espaco.m),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Semantics(header: true, child: Text('Recolhido em atendimentos concluídos', style: texto.titleLarge)),
            Text('Pela data de conclusão · $periodo', style: texto.bodySmall),
            const SizedBox(height: Espaco.m),
            if (resumo.quantidades.isEmpty)
              const EstadoVazio(icone: Icons.inventory_2_outlined, titulo: 'Nenhuma quantidade recolhida no período')
            else
              for (final q in resumo.quantidades)
                MergeSemantics(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: Espaco.xs),
                    child: Row(
                      children: [
                        IconeMaterial(nome: q.nome, tamanho: 28),
                        const SizedBox(width: Espaco.s),
                        Expanded(child: Text(q.nome, style: texto.bodyLarge)),
                        Text(q.quantidade.comUnidade(q.unidade), style: texto.titleMedium),
                      ],
                    ),
                  ),
                ),
          ],
        ),
      ),
    );
  }
}
