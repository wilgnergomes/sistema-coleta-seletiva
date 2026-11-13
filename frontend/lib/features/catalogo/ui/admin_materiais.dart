import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/cliente_api.dart';
import '../../../core/api/falhas.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/enums.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/envio.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../../../ui/compartilhado/paginador.dart';
import '../data/modelos.dart';
import '../data/repositorio_catalogo.dart';

/// `/administracao/materiais` (RF03): lista paginada com filtro de situação.
class PaginaMateriaisAdmin extends StatefulWidget {
  const PaginaMateriaisAdmin({
    super.key,
    required this.repositorio,
    required this.pagina,
    required this.situacao,
    required this.aoMudar,
    required this.aoAbrir,
    required this.aoNovo,
  });

  final RepositorioCatalogo repositorio;
  final int pagina;
  final SituacaoCadastro? situacao;
  final void Function(int pagina, SituacaoCadastro? situacao) aoMudar;

  /// Abre o editor; a lista é relida ao retornar.
  final Future<void> Function(int id) aoAbrir;
  final Future<void> Function() aoNovo;

  @override
  State<PaginaMateriaisAdmin> createState() => _PaginaMateriaisAdminState();
}

class _PaginaMateriaisAdminState extends State<PaginaMateriaisAdmin> {
  final _carga = Carga<Pagina<MaterialColeta>>();

  void _carregar() => _carga.executar(
    () => widget.repositorio.materiais(pagina: widget.pagina, acesso: Acesso.ator, situacao: widget.situacao?.api),
    () {
      if (mounted) setState(() {});
    },
  );

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _novo() async {
    await widget.aoNovo();
    if (mounted) _carregar();
  }

  @override
  void didUpdateWidget(PaginaMateriaisAdmin antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.pagina != widget.pagina || antigo.situacao != widget.situacao) _carregar();
  }

  @override
  Widget build(BuildContext context) {
    return PaginaRolavel(
      larguraMaxima: Largura.densa,
      filhos: [
        TituloPagina(
          'Materiais',
          subtitulo: 'Catálogo de materiais, unidades e orientações.',
          acao: FilledButton.icon(
            onPressed: widget.aoNovo,
            icon: const Icon(Icons.add),
            label: const Text('Novo material'),
          ),
        ),
        Wrap(
          spacing: Espaco.xs,
          children: [
            for (final s in <SituacaoCadastro?>[null, SituacaoCadastro.ativo, SituacaoCadastro.inativo])
              ChoiceChip(
                label: Text(s?.rotulo ?? 'Todas'),
                selected: s == widget.situacao,
                onSelected: (_) => widget.aoMudar(0, s),
              ),
          ],
        ),
        const SizedBox(height: Espaco.m),
        ConteudoCarga<Pagina<MaterialColeta>>(
          carga: _carga,
          aoTentarNovamente: _carregar,
          construir: (context, pagina) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('${pagina.totalElementos} materiais', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: Espaco.s),
              if (pagina.conteudo.isEmpty)
                EstadoVazio(
                  icone: Icons.recycling,
                  titulo: widget.situacao == null ? 'Nenhum material cadastrado' : 'Nenhum material nesta situação',
                  acao: widget.situacao == null
                      ? FilledButton(onPressed: _novo, child: const Text('Cadastrar material'))
                      : OutlinedButton(onPressed: () => widget.aoMudar(0, null), child: const Text('Limpar filtros')),
                )
              else
                GradeResponsiva(
                  larguraMinima: 300,
                  filhos: [
                    for (final m in pagina.conteudo)
                      Card(
                        key: ValueKey(m.id),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xs),
                          leading: IconeMaterial(nome: m.nome),
                          title: Text(m.nome),
                          subtitle: Padding(
                            padding: const EdgeInsets.only(top: Espaco.xxs),
                            child: Wrap(
                              spacing: Espaco.xs,
                              crossAxisAlignment: WrapCrossAlignment.center,
                              children: [
                                Text('Em ${m.unidade.extenso}'),
                                SeloCadastro(situacao: m.situacao),
                              ],
                            ),
                          ),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () async {
                            await widget.aoAbrir(m.id);
                            if (mounted) _carregar();
                          },
                        ),
                      ),
                  ],
                ),
              Paginador(
                pagina: pagina,
                desabilitado: _carga.carregando,
                aoMudar: (p) => widget.aoMudar(p, widget.situacao),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Editor de material. Na edição, `PUT` envia a representação completa.
class PaginaEditorMaterial extends StatefulWidget {
  const PaginaEditorMaterial({
    super.key,
    required this.repositorio,
    required this.aoVoltar,
    required this.aoSalvar,
    this.id,
  });

  final RepositorioCatalogo repositorio;
  final int? id;
  final VoidCallback aoVoltar;
  final ValueChanged<MaterialColeta> aoSalvar;

  @override
  State<PaginaEditorMaterial> createState() => _PaginaEditorMaterialState();
}

class _PaginaEditorMaterialState extends State<PaginaEditorMaterial> {
  final _form = GlobalKey<FormState>();
  final _nome = TextEditingController();
  final _descricao = TextEditingController();
  final _orientacao = TextEditingController();
  Unidade? _unidade;
  SituacaoCadastro _situacao = SituacaoCadastro.ativo;
  MaterialColeta? _original;
  final _carga = Carga<MaterialColeta>();
  final _envio = EstadoEnvio();

  @override
  void initState() {
    super.initState();
    if (widget.id != null) _carregar();
  }

  Future<void> _carregar() async {
    await _carga.executar(() => widget.repositorio.material(widget.id!, acesso: Acesso.ator), () {
      if (mounted) setState(() {});
    });
    final m = _carga.dados;
    if (m != null && mounted) {
      setState(() {
        _original = m;
        _nome.text = m.nome;
        _descricao.text = m.descricao;
        _orientacao.text = m.orientacao;
        _unidade = m.unidade == Unidade.desconhecida ? null : m.unidade;
        _situacao = m.situacao == SituacaoCadastro.inativo ? SituacaoCadastro.inativo : SituacaoCadastro.ativo;
      });
    }
  }

  @override
  void dispose() {
    for (final c in [_nome, _descricao, _orientacao]) {
      c.dispose();
    }
    _envio.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    if (!_form.currentState!.validate()) return;
    if (_original?.situacao == SituacaoCadastro.ativo && _situacao == SituacaoCadastro.inativo) {
      final ok = await confirmar(
        context,
        titulo: 'Inativar material',
        mensagem: 'O material ${_original!.nome} deixará de aparecer no catálogo público e em novas solicitações.',
        rotuloAcao: 'Inativar material',
        destrutiva: true,
      );
      if (!ok) return;
    }
    final dados = MaterialColeta(
      id: widget.id ?? 0,
      nome: _nome.text.trim(),
      descricao: _descricao.text.trim(),
      unidade: _unidade!,
      orientacao: _orientacao.text.trim(),
      situacao: _situacao,
    );
    final salvo = await _envio.executar(() => widget.repositorio.salvarMaterial(dados, id: widget.id));
    if (salvo != null) widget.aoSalvar(salvo);
  }

  @override
  Widget build(BuildContext context) {
    final falha = _carga.falha;
    final titulo = widget.id == null ? 'Novo material' : 'Editar material';
    if (widget.id != null && _original == null) {
      return PaginaRolavel(
        larguraMaxima: Largura.formulario,
        filhos: [
          TituloPagina(titulo),
          if (falha is ErroHttp && falha.naoEncontrado)
            EstadoVazio(
              titulo: 'Material não encontrado',
              acao: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar aos materiais')),
            )
          else if (falha != null)
            EstadoErro(mensagem: falha.mensagem, aoTentarNovamente: _carregar)
          else
            const EstadoCarregando(),
        ],
      );
    }
    return ListenableBuilder(
      listenable: _envio,
      builder: (context, _) => Form(
        key: _form,
        child: PaginaRolavel(
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
            TituloPagina(titulo),
            const LegendaObrigatorio(),
            MensagemEnvio(estado: _envio),
            CampoTexto(
              controlador: _nome,
              rotulo: 'Nome',
              obrigatorio: true,
              limite: 80,
              erroServidor: _envio.erro('nome'),
            ),
            CampoTexto(
              controlador: _descricao,
              rotulo: 'Descrição',
              obrigatorio: true,
              limite: 500,
              linhas: 2,
              erroServidor: _envio.erro('descricao'),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: Espaco.m),
              child: DropdownButtonFormField<Unidade>(
                initialValue: _unidade,
                decoration: InputDecoration(
                  labelText: 'Unidade *',
                  errorText: _envio.erro('unidade'),
                  helperText: _original == null
                      ? 'Peças (un.) só aceitam quantidades inteiras.'
                      : 'A unidade não muda se houver solicitações ou vínculos com este material.',
                  helperMaxLines: 2,
                ),
                items: [
                  for (final u in Unidade.conhecidas)
                    DropdownMenuItem(value: u, child: Text('${u.extenso} (${u.simbolo})')),
                ],
                validator: (v) => v == null ? 'Escolha a unidade.' : null,
                onChanged: (v) => setState(() => _unidade = v),
              ),
            ),
            CampoTexto(
              controlador: _orientacao,
              rotulo: 'Orientação de preparo',
              obrigatorio: true,
              limite: 1000,
              linhas: 4,
              erroServidor: _envio.erro('orientacao'),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: Espaco.m),
              child: DropdownButtonFormField<SituacaoCadastro>(
                initialValue: _situacao,
                decoration: InputDecoration(labelText: 'Situação *', errorText: _envio.erro('situacao')),
                items: const [
                  DropdownMenuItem(value: SituacaoCadastro.ativo, child: Text('Ativo')),
                  DropdownMenuItem(value: SituacaoCadastro.inativo, child: Text('Inativo')),
                ],
                onChanged: (v) => setState(() => _situacao = v ?? _situacao),
              ),
            ),
            Wrap(
              alignment: WrapAlignment.end,
              spacing: Espaco.s,
              children: [
                TextButton(onPressed: widget.aoVoltar, child: const Text('Voltar')),
                BotaoEnvio(rotulo: 'Salvar material', enviando: _envio.enviando, aoPressionar: _salvar),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
