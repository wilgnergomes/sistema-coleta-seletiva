import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';
import '../../core/api/cliente_api.dart';
import '../../core/api/falhas.dart';
import '../../core/api/json.dart';
import '../../core/api/pagina.dart';
import '../../core/dominio/enums.dart';
import '../../core/estado/carga.dart';
import '../../ui/compartilhado/campos.dart';
import '../../ui/compartilhado/dialogos.dart';
import '../../ui/compartilhado/envio.dart';
import '../../ui/compartilhado/estados.dart';
import '../../ui/compartilhado/layout.dart';
import '../../ui/compartilhado/paginador.dart';
import '../catalogo/data/repositorio_catalogo.dart';
import '../catalogo/ui/seletor_ponto.dart';
import '../../ui/compartilhado/superficie.dart';

class Operador {
  const Operador({
    required this.id,
    required this.nome,
    required this.email,
    required this.pontoId,
    required this.situacao,
  });

  factory Operador.deJson(Json j) => Operador(
    id: j.inteiro('id'),
    nome: j.texto('nome'),
    email: j.texto('email'),
    pontoId: j.inteiro('pontoId'),
    situacao: SituacaoCadastro.deApi(j.textoOpcional('situacao')),
  );

  final int id;
  final String nome;
  final String email;
  final int pontoId;
  final SituacaoCadastro situacao;
}

/// Operadores (RF07), exclusivos do administrador.
class RepositorioOperadores {
  RepositorioOperadores(this._api);

  final ClienteApi _api;

  Future<Pagina<Operador>> listar({int? pontoId, int pagina = 0}) async => Pagina.deJson(
    await _api.get('/operadores', query: {'pontoId': pontoId, 'page': pagina, 'size': 20}, acesso: Acesso.ator),
    Operador.deJson,
  );

  Future<Operador> obter(int id) async =>
      Operador.deJson(comoMapa(await _api.get('/operadores/$id', acesso: Acesso.ator)));

  Future<Operador> salvar({
    int? id,
    required String nome,
    required String email,
    required int pontoId,
    required SituacaoCadastro situacao,
  }) async {
    final corpo = {'nome': nome.trim(), 'email': email.trim(), 'pontoId': pontoId, 'situacao': situacao.api};
    final json = id == null
        ? await _api.post('/operadores', corpo: corpo)
        : await _api.put('/operadores/$id', corpo: corpo);
    return Operador.deJson(comoMapa(json));
  }
}

/// `/administracao/operadores`, com filtro por ponto preservado na URL.
class PaginaOperadores extends StatefulWidget {
  const PaginaOperadores({
    super.key,
    required this.repositorio,
    required this.catalogo,
    required this.pontoId,
    required this.pagina,
    required this.aoMudar,
    required this.aoAbrir,
    required this.aoNovo,
  });

  final RepositorioOperadores repositorio;
  final RepositorioCatalogo catalogo;
  final int? pontoId;
  final int pagina;
  final void Function(int? pontoId, int pagina) aoMudar;
  final Future<void> Function(int id) aoAbrir;
  final Future<void> Function() aoNovo;

  @override
  State<PaginaOperadores> createState() => _PaginaOperadoresState();
}

class _PaginaOperadoresState extends State<PaginaOperadores> {
  final _carga = Carga<Pagina<Operador>>();

  void _carregar() => _carga.executar(
    () async {
      final r = await Future.wait<Object?>([
        widget.repositorio.listar(pontoId: widget.pontoId, pagina: widget.pagina),
        widget.catalogo.carregarNomes(),
      ]);
      return r.first as Pagina<Operador>;
    },
    () {
      if (mounted) setState(() {});
    },
  );

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void didUpdateWidget(PaginaOperadores antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.pontoId != widget.pontoId || antigo.pagina != widget.pagina) _carregar();
  }

  Future<void> _abrir(Future<void> Function() acao) async {
    await acao();
    if (mounted) _carregar();
  }

  @override
  Widget build(BuildContext context) {
    return PaginaRolavel(
      larguraMaxima: Largura.densa,
      filhos: [
        TituloPagina(
          'Operadores',
          subtitulo: 'Cada operador atende a fila de um ponto de coleta.',
          acao: Elevavel(
            child: FilledButton.icon(
              onPressed: () => _abrir(widget.aoNovo),
              icon: const Icon(Icons.person_add_alt),
              label: const Text('Novo operador'),
            ),
          ),
        ),
        Wrap(
          spacing: Espaco.l,
          runSpacing: Espaco.s,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            InputChip(
              avatar: const Icon(Icons.place_outlined, size: 18),
              label: Text(
                widget.pontoId == null
                    ? 'Filtrar por ponto de coleta'
                    : 'Ponto: ${widget.catalogo.rotuloPonto(widget.pontoId!)}',
              ),
              selected: widget.pontoId != null,
              onPressed: () async {
                final p = await escolherPontoAdmin(context, widget.catalogo, selecionado: widget.pontoId);
                if (p != null) widget.aoMudar(p.id, 0);
              },
              onDeleted: widget.pontoId == null ? null : () => widget.aoMudar(null, 0),
              deleteButtonTooltipMessage: 'Remover filtro de ponto',
            ),
            AbrirPorNumero(rotulo: 'Abrir operador pelo número', aoAbrir: (id) => _abrir(() => widget.aoAbrir(id))),
          ],
        ),
        const SizedBox(height: Espaco.s),
        const Aviso(
          tipo: TipoAviso.info,
          mensagem:
              'O filtro usa o ponto de coleta (a pesquisa pelo nome do ponto acontece no seletor). '
              'A API ainda não oferece pesquisa pelo nome do operador.',
        ),
        const SizedBox(height: Espaco.m),
        ConteudoCarga<Pagina<Operador>>(
          carga: _carga,
          aoTentarNovamente: _carregar,
          construir: (context, pagina) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                pagina.totalElementos == 1 ? '1 operador' : '${pagina.totalElementos} operadores',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: Espaco.s),
              if (pagina.conteudo.isEmpty)
                EstadoVazio(
                  icone: Icons.badge_outlined,
                  titulo: widget.pontoId == null ? 'Nenhum operador cadastrado' : 'Nenhum operador neste ponto',
                )
              else
                GradeResponsiva(
                  larguraMinima: 300,
                  filhos: [
                    for (final o in pagina.conteudo)
                      CartaoInterativo(
                        key: ValueKey(o.id),
                        preenchimento: EdgeInsets.zero,
                        aoTocar: () => _abrir(() => widget.aoAbrir(o.id)),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xs),
                          title: Text(o.nome),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(o.email),
                              Text(widget.catalogo.rotuloPonto(o.pontoId)),
                              const SizedBox(height: Espaco.xxs),
                              SeloCadastro(situacao: o.situacao),
                            ],
                          ),
                          trailing: const Icon(Icons.chevron_right),
                        ),
                      ),
                  ],
                ),
              Paginador(
                pagina: pagina,
                desabilitado: _carga.carregando,
                aoMudar: (p) => widget.aoMudar(widget.pontoId, p),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Editor de operador com seletor de ponto (inclui inativos, que a API aceita).
class PaginaEditorOperador extends StatefulWidget {
  const PaginaEditorOperador({
    super.key,
    required this.repositorio,
    required this.catalogo,
    required this.aoVoltar,
    required this.aoSalvar,
    this.id,
  });

  final RepositorioOperadores repositorio;
  final RepositorioCatalogo catalogo;
  final int? id;
  final VoidCallback aoVoltar;
  final ValueChanged<Operador> aoSalvar;

  @override
  State<PaginaEditorOperador> createState() => _PaginaEditorOperadorState();
}

class _PaginaEditorOperadorState extends State<PaginaEditorOperador> {
  final _form = GlobalKey<FormState>();
  final _nome = TextEditingController();
  final _email = TextEditingController();
  int? _pontoId;
  String? _nomePonto;
  SituacaoCadastro _situacao = SituacaoCadastro.ativo;
  Operador? _original;
  final _carga = Carga<Operador>();
  final _envio = EstadoEnvio();
  String? _erroPonto;

  @override
  void initState() {
    super.initState();
    if (widget.id != null) _carregar();
  }

  Future<void> _carregar() async {
    await _carga.executar(
      () async {
        final r = await Future.wait<Object?>([widget.repositorio.obter(widget.id!), widget.catalogo.carregarNomes()]);
        return r.first as Operador;
      },
      () {
        if (mounted) setState(() {});
      },
    );
    final o = _carga.dados;
    if (o != null && mounted) {
      setState(() {
        _original = o;
        _nome.text = o.nome;
        _email.text = o.email;
        _pontoId = o.pontoId;
        _nomePonto = widget.catalogo.rotuloPonto(o.pontoId);
        _situacao = o.situacao == SituacaoCadastro.inativo ? SituacaoCadastro.inativo : SituacaoCadastro.ativo;
      });
    }
  }

  @override
  void dispose() {
    _nome.dispose();
    _email.dispose();
    _envio.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    final valido = _form.currentState!.validate();
    setState(() => _erroPonto = _pontoId == null ? 'Escolha o ponto do operador.' : null);
    if (!valido || _pontoId == null) return;
    final original = _original;
    if (original != null) {
      final mudancas = [
        if (original.situacao == SituacaoCadastro.ativo && _situacao == SituacaoCadastro.inativo)
          'o operador ficará inativo e não poderá agir sobre solicitações',
        if (original.pontoId != _pontoId)
          'a fila passará a ser a de ${_nomePonto ?? 'outro ponto'}, e as solicitações do ponto anterior deixam de ser visíveis para ele',
      ];
      if (mudancas.isNotEmpty) {
        final ok = await confirmar(
          context,
          titulo: 'Confirmar alteração',
          mensagem: 'Ao salvar, ${mudancas.join('; e ')}.',
          rotuloAcao: 'Salvar operador',
        );
        if (!ok) return;
      }
    }
    final salvo = await _envio.executar(
      () => widget.repositorio.salvar(
        id: widget.id,
        nome: _nome.text,
        email: _email.text,
        pontoId: _pontoId!,
        situacao: _situacao,
      ),
    );
    if (salvo != null) widget.aoSalvar(salvo);
  }

  @override
  Widget build(BuildContext context) {
    final titulo = widget.id == null ? 'Novo operador' : 'Editar operador';
    final falha = _carga.falha;
    if (widget.id != null && _original == null) {
      return PaginaRolavel(
        larguraMaxima: Largura.formulario,
        filhos: [
          TituloPagina(titulo),
          if (falha is ErroHttp && falha.naoEncontrado)
            EstadoVazio(
              titulo: 'Operador não encontrado',
              acao: Elevavel(
                child: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar aos operadores')),
              ),
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
              controlador: _email,
              rotulo: 'E-mail',
              obrigatorio: true,
              limite: 150,
              teclado: TextInputType.emailAddress,
              validar: validarEmail,
              erroServidor: _envio.erro('email'),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: Espaco.m),
              child: InputDecorator(
                decoration: InputDecoration(
                  labelText: 'Ponto de coleta *',
                  errorText: _erroPonto ?? _envio.erro('pontoId'),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(_pontoId == null ? 'Nenhum ponto escolhido' : (_nomePonto ?? 'Ponto #$_pontoId')),
                    ),
                    TextButton(
                      onPressed: () async {
                        final p = await escolherPontoAdmin(context, widget.catalogo, selecionado: _pontoId);
                        if (p != null) {
                          setState(() {
                            _pontoId = p.id;
                            _nomePonto = p.ativo ? p.nome : '${p.nome} (inativo)';
                            _erroPonto = null;
                          });
                        }
                      },
                      child: const Text('Escolher ponto'),
                    ),
                  ],
                ),
              ),
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
                BotaoEnvio(rotulo: 'Salvar operador', enviando: _envio.enviando, aoPressionar: _salvar),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
