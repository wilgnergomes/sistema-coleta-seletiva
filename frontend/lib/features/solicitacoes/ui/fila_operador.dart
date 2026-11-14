import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../ui/compartilhado/superficie.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/enums.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../data/solicitacoes.dart';
import 'acoes_operador.dart';
import 'componentes_solicitacao.dart';
import 'view_models.dart';

/// Largura mínima de conteúdo para lista e detalhe lado a lado.
const larguraFilaAmpla = 960.0;

/// Fila do ponto (RF07, RF09). No amplo, lista e detalhe ficam visíveis;
/// no compacto, o detalhe ocupa a tela e voltar restaura a consulta.
class PaginaFila extends StatefulWidget {
  const PaginaFila({
    super.key,
    required this.lista,
    required this.filtro,
    required this.selecionado,
    required this.criarDetalhe,
    required this.aoMudarFiltro,
    required this.aoSelecionar,
    required this.aoFecharDetalhe,
    this.reaproveitar,
  });

  final ListaSolicitacoesViewModel lista;
  final FiltroSolicitacoes filtro;
  final int? selecionado;
  final DetalheSolicitacaoViewModel Function(int id) criarDetalhe;
  final ValueChanged<FiltroSolicitacoes> aoMudarFiltro;
  final ValueChanged<int> aoSelecionar;
  final VoidCallback aoFecharDetalhe;

  /// Página já lida na validação do operador, para evitar nova chamada.
  final Pagina<SolicitacaoResumo>? reaproveitar;

  @override
  State<PaginaFila> createState() => _PaginaFilaState();
}

class _PaginaFilaState extends State<PaginaFila> {
  DetalheSolicitacaoViewModel? _detalhe;
  final _rolagemLista = ScrollController();

  ListaSolicitacoesViewModel get _lista => widget.lista;

  @override
  void initState() {
    super.initState();
    if (_lista.filtro != widget.filtro || _lista.carga.dados == null) {
      _lista.carregar(widget.filtro, reaproveitar: widget.reaproveitar);
    }
    _abrirDetalhe();
  }

  @override
  void didUpdateWidget(PaginaFila antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.filtro != widget.filtro) _lista.carregar(widget.filtro);
    if (antigo.selecionado != widget.selecionado) _abrirDetalhe();
  }

  void _abrirDetalhe() {
    final id = widget.selecionado;
    if (_detalhe?.id == id) return;
    _detalhe?.dispose();
    _detalhe = id == null ? null : (widget.criarDetalhe(id)..carregar());
  }

  @override
  void dispose() {
    _detalhe?.dispose();
    _rolagemLista.dispose();
    super.dispose();
  }

  Future<void> _periodo() async {
    final f = widget.filtro;
    final r = await escolherPeriodo(context, de: f.de, ate: f.ate);
    if (r == null) return;
    widget.aoMudarFiltro(FiltroSolicitacoes(situacao: f.situacao, de: r.$1, ate: r.$2));
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, restricoes) {
        final amplo = restricoes.maxWidth >= larguraFilaAmpla && restricoes.maxHeight >= 480;
        final detalhe = _detalhe;
        if (amplo) {
          return Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(width: 352, child: _listaRolavel(context, compacta: false)),
              // O detalhe é a área principal: superfície elevada e larga, com
              // transição suave ao trocar de solicitação.
              Expanded(
                child: Container(
                  color: Cores.fundoVerde,
                  padding: const EdgeInsets.fromLTRB(Espaco.s, Espaco.m, Espaco.m, Espaco.m),
                  child: AnimatedSwitcher(
                    duration: duracao(context, 260),
                    switchInCurve: Curves.easeOutCubic,
                    child: Container(
                      key: ValueKey(detalhe?.id),
                      decoration: BoxDecoration(
                        color: Cores.superficie,
                        borderRadius: BorderRadius.circular(Raio.dialogo),
                        boxShadow: Sombra.elevada,
                      ),
                      clipBehavior: Clip.antiAlias,
                      child: detalhe == null
                          ? const Center(
                              child: EstadoVazio(
                                icone: Icons.touch_app_outlined,
                                titulo: 'Selecione uma solicitação',
                                mensagem: 'O detalhe, as ações e o histórico aparecem aqui.',
                              ),
                            )
                          : PainelDetalheOperador(viewModel: detalhe, aoAlterar: () => _lista.carregar(widget.filtro)),
                    ),
                  ),
                ),
              ),
            ],
          );
        }
        if (detalhe != null) {
          return PainelDetalheOperador(
            key: ValueKey(detalhe.id),
            viewModel: detalhe,
            aoVoltar: widget.aoFecharDetalhe,
            aoAlterar: () => _lista.carregar(widget.filtro),
          );
        }
        return _listaRolavel(context, compacta: true);
      },
    );
  }

  Widget _listaRolavel(BuildContext context, {required bool compacta}) {
    final f = widget.filtro;
    return ListenableBuilder(
      listenable: _lista,
      builder: (context, _) => PaginaRolavel(
        controlador: _rolagemLista,
        larguraMaxima: compacta ? Largura.formulario : 360,
        filhos: [
          const TituloPagina('Fila do ponto', subtitulo: 'Solicitações do ponto ao qual você está vinculado.'),
          FiltroSituacao(selecionada: f.situacao, aoMudar: (s) => widget.aoMudarFiltro(f.comSituacao(s))),
          const SizedBox(height: Espaco.s),
          Wrap(
            spacing: Espaco.s,
            runSpacing: Espaco.xs,
            children: [
              OutlinedButton.icon(
                onPressed: _periodo,
                icon: const Icon(Icons.date_range_outlined),
                label: Text('Filtros de data · ${rotuloPeriodo(f.de, f.ate)}'),
              ),
              IconButton(
                tooltip: 'Atualizar a fila',
                onPressed: _lista.carga.carregando ? null : () => _lista.carregar(f),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          const SizedBox(height: Espaco.m),
          ConteudoCarga<Pagina<SolicitacaoResumo>>(
            carga: _lista.carga,
            rotuloCarregando: 'Carregando a fila…',
            aoTentarNovamente: () => _lista.carregar(f),
            construir: (context, pagina) => _itens(context, pagina, f),
          ),
        ],
      ),
    );
  }

  Widget _itens(BuildContext context, Pagina<SolicitacaoResumo> pagina, FiltroSolicitacoes f) {
    if (pagina.alemDaUltima) {
      WidgetsBinding.instance.addPostFrameCallback((_) => widget.aoMudarFiltro(f.naPagina(pagina.totalPaginas - 1)));
    }
    final total = pagina.totalElementos;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Semantics(
          liveRegion: true,
          child: Text(
            total == 1 ? '1 solicitação no filtro' : '$total solicitações no filtro',
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ),
        const SizedBox(height: Espaco.s),
        if (pagina.conteudo.isEmpty)
          EstadoVazio(
            icone: Icons.inbox_outlined,
            titulo: f.situacao == SituacaoSolicitacao.solicitada
                ? 'Nenhuma solicitação pendente'
                : 'Nenhuma solicitação no filtro',
            acao: f.situacao == null && f.de == null && f.ate == null
                ? null
                : OutlinedButton(
                    onPressed: () => widget.aoMudarFiltro(const FiltroSolicitacoes()),
                    child: const Text('Ver todas'),
                  ),
          )
        else
          for (final s in pagina.conteudo)
            Padding(
              key: ValueKey(s.id),
              padding: const EdgeInsets.only(bottom: Espaco.s),
              child: CartaoSolicitacao(
                solicitacao: s,
                nomePonto: _lista.nomePonto(s.pontoId),
                fuso: _lista.fuso,
                selecionada: s.id == widget.selecionado,
                aoAbrir: () => widget.aoSelecionar(s.id),
              ),
            ),
        Paginador(
          pagina: pagina,
          desabilitado: _lista.carga.carregando,
          aoMudar: (p) => widget.aoMudarFiltro(f.naPagina(p)),
        ),
      ],
    );
  }
}

/// Detalhe operacional com ações por situação e histórico no mesmo contexto.
class PainelDetalheOperador extends StatefulWidget {
  const PainelDetalheOperador({super.key, required this.viewModel, required this.aoAlterar, this.aoVoltar, this.agora});

  final DetalheSolicitacaoViewModel viewModel;

  /// Chamado após ação confirmada, para atualizar a fila.
  final VoidCallback aoAlterar;
  final VoidCallback? aoVoltar;

  /// Relógio injetável para testes.
  final DateTime Function()? agora;

  @override
  State<PainelDetalheOperador> createState() => _PainelDetalheOperadorState();
}

class _PainelDetalheOperadorState extends State<PainelDetalheOperador> {
  final _chaveHistorico = GlobalKey();

  DetalheSolicitacaoViewModel get _vm => widget.viewModel;

  void _verHistorico() {
    final contexto = _chaveHistorico.currentContext;
    if (contexto != null) {
      Scrollable.ensureVisible(contexto, duration: const Duration(milliseconds: 200));
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: _vm,
      builder: (context, _) {
        if (_vm.indisponivel) {
          return PaginaRolavel(
            larguraMaxima: Largura.formulario,
            filhos: [
              EstadoVazio(
                icone: Icons.search_off,
                titulo: 'Solicitação indisponível',
                mensagem: _vm.carga.falha?.mensagem,
                acao: widget.aoVoltar == null
                    ? null
                    : Elevavel(
                        child: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar para fila')),
                      ),
              ),
            ],
          );
        }
        return PaginaRolavel(
          larguraMaxima: Largura.formulario,
          filhos: [
            Wrap(
              spacing: Espaco.s,
              children: [
                if (widget.aoVoltar != null)
                  TextButton.icon(
                    onPressed: widget.aoVoltar,
                    icon: const Icon(Icons.arrow_back),
                    label: const Text('Voltar para fila'),
                  ),
                if (_vm.carga.dados != null)
                  TextButton.icon(
                    onPressed: _verHistorico,
                    icon: const Icon(Icons.history),
                    label: const Text('Ver histórico'),
                  ),
              ],
            ),
            ConteudoCarga<SolicitacaoDetalhe>(
              carga: _vm.carga,
              rotuloCarregando: 'Carregando solicitação…',
              aoTentarNovamente: _vm.carregar,
              construir: (context, d) => DetalheSolicitacaoView(
                detalhe: d,
                nomePonto: _vm.nomePonto,
                fuso: _vm.fuso,
                chaveHistorico: _chaveHistorico,
                mostrarMorador: true,
                aviso: _vm.aviso == null ? null : Aviso(tipo: _vm.tipoAviso, mensagem: _vm.aviso!),
                acoes: AcoesOperador(viewModel: _vm, aoAlterar: widget.aoAlterar, agora: widget.agora),
              ),
            ),
          ],
        );
      },
    );
  }
}
