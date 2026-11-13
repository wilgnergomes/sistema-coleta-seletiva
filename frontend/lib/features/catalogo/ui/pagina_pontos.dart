import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/cliente_api.dart';
import '../../../core/api/pagina.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../data/modelos.dart';
import '../data/repositorio_catalogo.dart';
import 'componentes_catalogo.dart';

class PontosViewModel extends ChangeNotifier {
  PontosViewModel(this._repositorio, {this.acesso = Acesso.publico});

  final RepositorioCatalogo _repositorio;
  final Acesso acesso;
  final resultados = Carga<Pagina<PontoColeta>>();
  final materiais = Carga<CatalogoMateriais>();
  FiltroPontos? filtro;

  Future<void> carregar(FiltroPontos novo, {bool forcar = false}) {
    filtro = novo;
    return resultados.executar(
      () => _repositorio.pesquisarPontos(novo, acesso: acesso, forcar: forcar),
      notifyListeners,
    );
  }

  Future<void> carregarMateriais() => materiais.executar(_repositorio.materiaisAtivos, notifyListeners);

  String? nomeMaterial(int? id) => id == null ? null : materiais.dados?.porId(id)?.nome;
}

/// Pesquisa de pontos (RF06). Os filtros aplicados e a página vivem na URL;
/// o texto ainda em edição fica na tela até `Buscar` ou `Aplicar filtros`.
class PaginaPontos extends StatefulWidget {
  const PaginaPontos({
    super.key,
    required this.filtro,
    required this.repositorio,
    required this.aoMudarFiltro,
    required this.aoAbrirPonto,
    this.acesso = Acesso.publico,
    this.titulo = 'Pontos de coleta',
    this.subtitulo = 'Encontre onde entregar recicláveis ou quem faz recolhimento domiciliar.',
    this.administrativo = false,
    this.acaoTitulo,
  });

  final FiltroPontos filtro;
  final RepositorioCatalogo repositorio;
  final ValueChanged<FiltroPontos> aoMudarFiltro;

  /// Abre o detalhe. Na administração, a lista é relida ao retornar.
  final Future<void> Function(int id) aoAbrirPonto;
  final Acesso acesso;
  final String titulo;
  final String subtitulo;
  final bool administrativo;
  final Widget? acaoTitulo;

  @override
  State<PaginaPontos> createState() => _PaginaPontosState();
}

class _PaginaPontosState extends State<PaginaPontos> {
  late final _vm = PontosViewModel(widget.repositorio, acesso: widget.acesso);
  late final _nome = TextEditingController(text: widget.filtro.nome);
  final _focoResultados = FocusNode(debugLabel: 'resultados');

  @override
  void initState() {
    super.initState();
    _vm.carregar(widget.filtro);
    _vm.carregarMateriais();
  }

  @override
  void didUpdateWidget(PaginaPontos antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.filtro != widget.filtro) {
      if (antigo.filtro.nome != widget.filtro.nome) _nome.text = widget.filtro.nome ?? '';
      _vm.carregar(widget.filtro);
      if (antigo.filtro.pagina != widget.filtro.pagina) {
        WidgetsBinding.instance.addPostFrameCallback((_) => _focoResultados.requestFocus());
      }
    }
  }

  @override
  void dispose() {
    _vm.dispose();
    _nome.dispose();
    _focoResultados.dispose();
    super.dispose();
  }

  void _aplicar(FiltroPontos novo) => widget.aoMudarFiltro(novo.naPagina(0));

  Future<void> _abrir(int id) async {
    await widget.aoAbrirPonto(id);
    if (mounted && widget.administrativo) await _vm.carregar(widget.filtro, forcar: true);
  }

  void _buscar(String texto) {
    final f = widget.filtro;
    final nome = texto.trim().isEmpty ? null : texto.trim();
    _aplicar(
      FiltroPontos(
        nome: nome,
        municipio: f.municipio,
        bairro: f.bairro,
        materialId: f.materialId,
        situacao: f.situacao,
      ),
    );
  }

  Future<void> _abrirFiltros() async {
    final f = widget.filtro;
    final atual = FiltroPontos(
      nome: _nome.text.trim().isEmpty ? null : _nome.text.trim(),
      municipio: f.municipio,
      bairro: f.bairro,
      materialId: f.materialId,
      situacao: f.situacao,
    );
    final novo = await abrirFiltrosPontos(
      context,
      atual: atual,
      catalogo: _vm.materiais.dados,
      permitirSituacao: widget.administrativo,
    );
    if (novo != null) _aplicar(novo);
  }

  void _material(MaterialColeta? m) {
    final f = widget.filtro;
    _aplicar(
      FiltroPontos(nome: f.nome, municipio: f.municipio, bairro: f.bairro, materialId: m?.id, situacao: f.situacao),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: _vm,
      builder: (context, _) {
        final f = widget.filtro;
        final pagina = _vm.resultados.dados;
        if (pagina != null && pagina.alemDaUltima && !_vm.resultados.carregando) {
          WidgetsBinding.instance.addPostFrameCallback(
            (_) => widget.aoMudarFiltro(f.naPagina(pagina.totalPaginas - 1)),
          );
        }
        final secundarios = [f.municipio, f.bairro, f.materialId, f.situacao].where((v) => v != null).length;
        return PaginaRolavel(
          filhos: [
            TituloPagina(widget.titulo, subtitulo: widget.subtitulo, acao: widget.acaoTitulo),
            BarraPesquisa(
              controlador: _nome,
              aoPesquisar: _buscar,
              aoAbrirFiltros: _abrirFiltros,
              quantidadeFiltros: secundarios,
            ),
            const SizedBox(height: Espaco.s),
            AtalhosMateriais(catalogo: _vm.materiais.dados, selecionado: f.materialId, aoSelecionar: _material),
            if (f.temFiltros) ...[const SizedBox(height: Espaco.s), _filtrosAplicados(f)],
            const SizedBox(height: Espaco.m),
            ConteudoCarga<Pagina<PontoColeta>>(
              carga: _vm.resultados,
              rotuloCarregando: 'Carregando pontos de coleta…',
              aoTentarNovamente: () => _vm.carregar(f, forcar: true),
              construir: (context, pagina) => _resultados(context, pagina, f),
            ),
          ],
        );
      },
    );
  }

  Widget _filtrosAplicados(FiltroPontos f) {
    Widget chip(String rotulo, FiltroPontos semEste) => InputChip(
      label: Text(rotulo),
      onDeleted: () => _aplicar(semEste),
      deleteButtonTooltipMessage: 'Remover filtro $rotulo',
    );
    FiltroPontos copia({
      bool nome = true,
      bool municipio = true,
      bool bairro = true,
      bool material = true,
      bool situacao = true,
    }) => FiltroPontos(
      nome: nome ? f.nome : null,
      municipio: municipio ? f.municipio : null,
      bairro: bairro ? f.bairro : null,
      materialId: material ? f.materialId : null,
      situacao: situacao ? f.situacao : null,
    );
    final material = _vm.nomeMaterial(f.materialId);
    return Wrap(
      spacing: Espaco.xs,
      runSpacing: Espaco.xs,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        if (f.nome != null) chip('Nome: ${f.nome}', copia(nome: false)),
        if (f.municipio != null) chip('Município: ${f.municipio}', copia(municipio: false)),
        if (f.bairro != null) chip('Bairro: ${f.bairro}', copia(bairro: false)),
        if (f.materialId != null) chip('Material: ${material ?? '#${f.materialId}'}', copia(material: false)),
        if (f.situacao != null) chip('Situação: ${f.situacao!.rotulo}', copia(situacao: false)),
        TextButton(
          onPressed: () {
            _nome.clear();
            _aplicar(const FiltroPontos());
          },
          child: const Text('Limpar filtros'),
        ),
      ],
    );
  }

  Widget _resultados(BuildContext context, Pagina<PontoColeta> pagina, FiltroPontos f) {
    final materialIndisponivel =
        f.materialId != null &&
        _vm.materiais.dados != null &&
        _vm.materiais.dados!.completo &&
        _vm.nomeMaterial(f.materialId) == null &&
        !widget.administrativo;
    final total = pagina.totalElementos;
    final rotuloTotal = total == 1 ? '1 ponto de coleta' : '$total pontos de coleta';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (materialIndisponivel)
          Padding(
            padding: const EdgeInsets.only(bottom: Espaco.m),
            child: Aviso(
              tipo: TipoAviso.alerta,
              mensagem: 'O material escolhido não está disponível no catálogo.',
              acao: TextButton(onPressed: () => _aplicar(f.semMaterial()), child: const Text('Limpar material')),
            ),
          ),
        Focus(
          focusNode: _focoResultados,
          child: Semantics(
            liveRegion: true,
            header: true,
            child: Text(
              f.temFiltros ? '$rotuloTotal encontrado${total == 1 ? '' : 's'}' : rotuloTotal,
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
        ),
        const SizedBox(height: Espaco.s),
        if (pagina.conteudo.isEmpty)
          f.temFiltros
              ? EstadoVazio(
                  icone: Icons.search_off,
                  titulo: 'Nenhum ponto corresponde aos filtros',
                  mensagem: 'Confira a grafia do município e do bairro ou remova algum filtro.',
                  acao: OutlinedButton(
                    onPressed: () {
                      _nome.clear();
                      _aplicar(const FiltroPontos());
                    },
                    child: const Text('Limpar filtros'),
                  ),
                )
              : const EstadoVazio(
                  icone: Icons.location_off_outlined,
                  titulo: 'Nenhum ponto de coleta cadastrado',
                  mensagem: 'Quando houver pontos ativos, eles aparecerão aqui.',
                )
        else
          GradeResponsiva(
            filhos: [
              for (final p in pagina.conteudo)
                CartaoPonto(
                  key: ValueKey(p.id),
                  ponto: p,
                  mostrarSituacao: widget.administrativo,
                  aoAbrir: () => _abrir(p.id),
                ),
            ],
          ),
        Paginador(
          pagina: pagina,
          desabilitado: _vm.resultados.carregando,
          aoMudar: (p) => widget.aoMudarFiltro(f.naPagina(p)),
        ),
      ],
    );
  }
}
