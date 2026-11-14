import 'package:material_ui/material_ui.dart';

import '../../../app/sessao.dart';
import '../../../app/tema/tokens.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../data/moradores.dart';
import 'enderecos.dart';
import 'formulario_morador.dart';

/// Cadastro público de morador (RF01). Sem campo de situação.
class PaginaCadastro extends StatefulWidget {
  const PaginaCadastro({
    super.key,
    required this.repositorio,
    required this.demonstracao,
    required this.aoUsarNaDemonstracao,
    required this.aoVoltarAoCatalogo,
  });

  final RepositorioMoradores repositorio;
  final bool demonstracao;
  final ValueChanged<Morador> aoUsarNaDemonstracao;
  final VoidCallback aoVoltarAoCatalogo;

  @override
  State<PaginaCadastro> createState() => _PaginaCadastroState();
}

class _PaginaCadastroState extends State<PaginaCadastro> {
  Morador? _criado;

  @override
  Widget build(BuildContext context) {
    final criado = _criado;
    return PaginaRolavel(
      larguraMaxima: Largura.formulario,
      filhos: [
        const TituloPagina(
          'Cadastrar morador',
          subtitulo: 'Com o cadastro, você pode solicitar recolhimento domiciliar de recicláveis.',
        ),
        if (criado == null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(Espaco.l),
              child: FormularioMorador(
                rotuloSalvar: 'Cadastrar morador',
                salvar: widget.repositorio.cadastrar,
                aoSalvar: (m) => setState(() => _criado = m),
                aoVoltar: widget.aoVoltarAoCatalogo,
              ),
            ),
          )
        else ...[
          Aviso(
            tipo: TipoAviso.sucesso,
            titulo: 'Cadastro concluído',
            mensagem: '${criado.nome}, seu cadastro foi criado com o número ${criado.id}.',
          ),
          const SizedBox(height: Espaco.l),
          Wrap(
            spacing: Espaco.s,
            runSpacing: Espaco.s,
            children: [
              if (widget.demonstracao)
                FilledButton.icon(
                  onPressed: () => widget.aoUsarNaDemonstracao(criado),
                  icon: const Icon(Icons.person_pin_outlined),
                  label: const Text('Usar este cadastro na demonstração'),
                ),
              OutlinedButton(onPressed: widget.aoVoltarAoCatalogo, child: const Text('Voltar aos pontos de coleta')),
            ],
          ),
        ],
      ],
    );
  }
}

/// Conta do próprio morador: dados pessoais, endereços e sessão.
class PaginaConta extends StatefulWidget {
  const PaginaConta({
    super.key,
    required this.sessao,
    required this.repositorio,
    required this.aoAbrirEnderecos,
    required this.aoTrocarIdentidade,
    required this.aoSairDaDemonstracao,
  });

  final Sessao sessao;
  final RepositorioMoradores repositorio;
  final VoidCallback aoAbrirEnderecos;
  final VoidCallback aoTrocarIdentidade;
  final VoidCallback aoSairDaDemonstracao;

  @override
  State<PaginaConta> createState() => _PaginaContaState();
}

class _PaginaContaState extends State<PaginaConta> {
  final _carga = Carga<Morador>();
  bool _editando = false;

  int get _id => widget.sessao.usuarioId!;

  void _carregar() => _carga.executar(() => widget.repositorio.obter(_id), () {
    if (mounted) setState(() {});
  });

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return PaginaRolavel(
      larguraMaxima: Largura.formulario,
      filhos: [
        const TituloPagina('Minha conta'),
        ConteudoCarga<Morador>(
          carga: _carga,
          aoTentarNovamente: _carregar,
          construir: (context, m) => Card(
            child: Padding(
              padding: const EdgeInsets.all(Espaco.l),
              child: _editando
                  ? FormularioMorador(
                      inicial: m,
                      rotuloSalvar: 'Salvar dados',
                      salvar: (dados) => widget.repositorio.atualizar(_id, dados),
                      aoVoltar: () => setState(() => _editando = false),
                      aoSalvar: (salvo) {
                        _carga.definir(salvo);
                        widget.sessao.atualizarNome(salvo.nome);
                        setState(() => _editando = false);
                        mostrarMensagem(context, 'Dados atualizados.');
                      },
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text('Dados pessoais', style: texto.titleLarge),
                        const SizedBox(height: Espaco.m),
                        CampoLeitura(rotulo: 'Nome', valor: m.nome),
                        CampoLeitura(rotulo: 'E-mail', valor: m.email),
                        CampoLeitura(rotulo: 'Telefone', valor: m.fone ?? 'Não informado'),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: OutlinedButton.icon(
                            onPressed: () => setState(() => _editando = true),
                            icon: const Icon(Icons.edit_outlined),
                            label: const Text('Editar dados'),
                          ),
                        ),
                      ],
                    ),
            ),
          ),
        ),
        const SizedBox(height: Espaco.m),
        Card(
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.l, vertical: Espaco.xs),
            leading: const Icon(Icons.home_work_outlined),
            title: const Text('Meus endereços'),
            subtitle: const Text('Locais usados nas solicitações de recolhimento'),
            trailing: const Icon(Icons.chevron_right),
            onTap: widget.aoAbrirEnderecos,
          ),
        ),
        const SizedBox(height: Espaco.m),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(Espaco.l),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Configurações', style: texto.titleLarge),
                const SizedBox(height: Espaco.s),
                Text(
                  'O aplicativo segue o tamanho de texto, o zoom e a redução de movimento do seu dispositivo ou navegador.',
                  style: texto.bodyMedium,
                ),
                const SizedBox(height: Espaco.m),
                Text('Identidade de demonstração', style: texto.titleMedium),
                const SizedBox(height: Espaco.xxs),
                Text(widget.sessao.ator?.descricao ?? '', style: texto.bodyMedium),
                const SizedBox(height: Espaco.s),
                Wrap(
                  spacing: Espaco.s,
                  runSpacing: Espaco.s,
                  children: [
                    OutlinedButton.icon(
                      onPressed: widget.aoTrocarIdentidade,
                      icon: const Icon(Icons.switch_account_outlined),
                      label: const Text('Trocar perfil'),
                    ),
                    OutlinedButton.icon(
                      onPressed: widget.aoSairDaDemonstracao,
                      icon: const Icon(Icons.logout),
                      label: const Text('Sair da demonstração'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class PaginaEnderecos extends StatelessWidget {
  const PaginaEnderecos({super.key, required this.moradorId, required this.repositorio, required this.aoVoltar});

  final int moradorId;
  final RepositorioMoradores repositorio;
  final VoidCallback aoVoltar;

  @override
  Widget build(BuildContext context) {
    return PaginaRolavel(
      larguraMaxima: Largura.formulario,
      filhos: [
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(onPressed: aoVoltar, icon: const Icon(Icons.arrow_back), label: const Text('Voltar')),
        ),
        const TituloPagina('Meus endereços'),
        SecaoEnderecos(moradorId: moradorId, repositorio: repositorio),
      ],
    );
  }
}
