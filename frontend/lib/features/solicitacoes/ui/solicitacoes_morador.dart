import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/enums.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../data/solicitacoes.dart';
import 'componentes_solicitacao.dart';
import 'view_models.dart';

/// Minhas solicitações (RF09) com filtros de situação e período na URL.
class PaginaSolicitacoes extends StatefulWidget {
  const PaginaSolicitacoes({
    super.key,
    required this.viewModel,
    required this.filtro,
    required this.aoMudarFiltro,
    required this.aoAbrir,
    this.titulo = 'Minhas solicitações',
    this.acaoVazia,
    this.mostrarMorador = false,
    this.filtrosExtras,
  });

  final ListaSolicitacoesViewModel viewModel;
  final FiltroSolicitacoes filtro;
  final ValueChanged<FiltroSolicitacoes> aoMudarFiltro;
  final ValueChanged<int> aoAbrir;
  final String titulo;
  final Widget? acaoVazia;
  final bool mostrarMorador;

  /// Filtros adicionais do contexto, como o ponto na administração.
  final Widget? filtrosExtras;

  @override
  State<PaginaSolicitacoes> createState() => _PaginaSolicitacoesState();
}

class _PaginaSolicitacoesState extends State<PaginaSolicitacoes> {
  /// Fixado na criação: o roteador pode reconstruir a rota (por exemplo, na
  /// troca do endereço selecionado) e entregar outra instância ainda vazia.
  late final ListaSolicitacoesViewModel _vm = widget.viewModel;

  @override
  void initState() {
    super.initState();
    _vm.carregar(widget.filtro);
  }

  @override
  void didUpdateWidget(PaginaSolicitacoes antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.filtro != widget.filtro) _vm.carregar(widget.filtro);
  }

  Future<void> _periodo() async {
    final f = widget.filtro;
    final r = await escolherPeriodo(context, de: f.de, ate: f.ate);
    if (r == null) return;
    widget.aoMudarFiltro(FiltroSolicitacoes(situacao: f.situacao, pontoId: f.pontoId, de: r.$1, ate: r.$2));
  }

  @override
  Widget build(BuildContext context) {
    final f = widget.filtro;
    return ListenableBuilder(
      listenable: _vm,
      builder: (context, _) => PaginaRolavel(
        larguraMaxima: Largura.densa,
        filhos: [
          TituloPagina(widget.titulo, subtitulo: 'Ordenadas da criação mais recente para a mais antiga.'),
          FiltroSituacao(selecionada: f.situacao, aoMudar: (s) => widget.aoMudarFiltro(f.comSituacao(s))),
          const SizedBox(height: Espaco.s),
          Wrap(
            spacing: Espaco.s,
            runSpacing: Espaco.s,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              OutlinedButton.icon(
                onPressed: _periodo,
                icon: const Icon(Icons.date_range_outlined),
                label: Text('Criação: ${rotuloPeriodo(f.de, f.ate)}'),
              ),
              ?widget.filtrosExtras,
              if (f.temFiltros)
                TextButton(
                  onPressed: () => widget.aoMudarFiltro(const FiltroSolicitacoes()),
                  child: const Text('Limpar filtros'),
                ),
            ],
          ),
          const SizedBox(height: Espaco.m),
          ConteudoCarga<Pagina<SolicitacaoResumo>>(
            carga: _vm.carga,
            rotuloCarregando: 'Carregando solicitações…',
            aoTentarNovamente: () => _vm.carregar(f),
            construir: (context, pagina) => _lista(context, pagina, f),
          ),
        ],
      ),
    );
  }

  Widget _lista(BuildContext context, Pagina<SolicitacaoResumo> pagina, FiltroSolicitacoes f) {
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
            total == 1 ? '1 solicitação' : '$total solicitações',
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ),
        const SizedBox(height: Espaco.s),
        if (pagina.conteudo.isEmpty)
          f.temFiltros
              ? EstadoVazio(
                  icone: Icons.filter_alt_off_outlined,
                  titulo: 'Nenhuma solicitação corresponde aos filtros',
                  acao: OutlinedButton(
                    onPressed: () => widget.aoMudarFiltro(const FiltroSolicitacoes()),
                    child: const Text('Limpar filtros'),
                  ),
                )
              : EstadoVazio(
                  icone: Icons.receipt_long_outlined,
                  titulo: 'Você ainda não tem solicitações',
                  acao: widget.acaoVazia,
                )
        else
          GradeResponsiva(
            larguraMinima: 320,
            filhos: [
              for (final s in pagina.conteudo)
                CartaoSolicitacao(
                  key: ValueKey(s.id),
                  solicitacao: s,
                  nomePonto: _vm.nomePonto(s.pontoId),
                  fuso: _vm.fuso,
                  mostrarMorador: widget.mostrarMorador,
                  aoAbrir: () => widget.aoAbrir(s.id),
                ),
            ],
          ),
        Paginador(
          pagina: pagina,
          desabilitado: _vm.carga.carregando,
          aoMudar: (p) => widget.aoMudarFiltro(f.naPagina(p)),
        ),
      ],
    );
  }
}

/// Detalhe da solicitação do morador: corrigir em `SOLICITADA` e cancelar
/// em `SOLICITADA` ou `AGENDADA` (RF08, RF12, RN04).
class PaginaDetalheSolicitacaoMorador extends StatefulWidget {
  const PaginaDetalheSolicitacaoMorador({
    super.key,
    required this.viewModel,
    required this.aoVoltar,
    required this.aoCorrigir,
    this.mensagemInicial,
  });

  final DetalheSolicitacaoViewModel viewModel;
  final VoidCallback aoVoltar;
  final ValueChanged<int> aoCorrigir;
  final String? mensagemInicial;

  @override
  State<PaginaDetalheSolicitacaoMorador> createState() => _PaginaDetalheSolicitacaoMoradorState();
}

class _PaginaDetalheSolicitacaoMoradorState extends State<PaginaDetalheSolicitacaoMorador> {
  /// Fixado na criação: o roteador pode reconstruir a rota (por exemplo, na
  /// troca do endereço selecionado) e entregar outra instância ainda vazia.
  late final DetalheSolicitacaoViewModel _vm = widget.viewModel;
  String? _rascunhoMotivo;

  @override
  void initState() {
    super.initState();
    if (widget.mensagemInicial != null) {
      _vm
        ..aviso = widget.mensagemInicial
        ..tipoAviso = TipoAviso.sucesso;
    }
    _vm.carregar();
  }

  @override
  void dispose() {
    _vm.dispose();
    super.dispose();
  }

  Future<void> _cancelar(SolicitacaoDetalhe d) async {
    final motivo = await pedirMotivo(
      context,
      titulo: 'Cancelar solicitação',
      descricao:
          'A solicitação ${d.protocolo} será cancelada e não poderá ser retomada.'
          '${d.agendadaPara == null ? '' : ' A data agendada continua registrada no histórico.'}',
      rotuloAcao: 'Cancelar solicitação',
      inicial: _rascunhoMotivo,
    );
    if (motivo == null) return;
    _rascunhoMotivo = motivo;
    final ok = await _vm.executar(
      (atual) => _vm.repositorio.cancelar(atual.id, motivo, atual.versao),
      sucesso: 'Solicitação cancelada.',
    );
    if (ok) _rascunhoMotivo = null;
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
                acao: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar às solicitações')),
              ),
            ],
          );
        }
        return PaginaRolavel(
          larguraMaxima: Largura.formulario,
          filhos: [
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: widget.aoVoltar,
                icon: const Icon(Icons.arrow_back),
                label: const Text('Voltar'),
              ),
            ),
            ConteudoCarga<SolicitacaoDetalhe>(
              carga: _vm.carga,
              rotuloCarregando: 'Carregando solicitação…',
              aoTentarNovamente: _vm.carregar,
              construir: (context, d) => DetalheSolicitacaoView(
                detalhe: d,
                nomePonto: _vm.nomePonto,
                fuso: _vm.fuso,
                aviso: _vm.aviso == null ? null : Aviso(tipo: _vm.tipoAviso, mensagem: _vm.aviso!),
                acoes: _acoes(d),
              ),
            ),
          ],
        );
      },
    );
  }

  Widget? _acoes(SolicitacaoDetalhe d) {
    final corrigir = d.situacao == SituacaoSolicitacao.solicitada;
    final cancelar = d.situacao == SituacaoSolicitacao.solicitada || d.situacao == SituacaoSolicitacao.agendada;
    if (!corrigir && !cancelar) return null;
    return Wrap(
      spacing: Espaco.s,
      runSpacing: Espaco.s,
      children: [
        if (corrigir)
          FilledButton.icon(
            onPressed: _vm.enviando ? null : () => widget.aoCorrigir(d.id),
            icon: const Icon(Icons.edit_outlined),
            label: const Text('Corrigir solicitação'),
          ),
        if (cancelar)
          OutlinedButton.icon(
            onPressed: _vm.enviando ? null : () => _cancelar(d),
            icon: const Icon(Icons.cancel_outlined),
            label: const Text('Cancelar solicitação'),
          ),
      ],
    );
  }
}
