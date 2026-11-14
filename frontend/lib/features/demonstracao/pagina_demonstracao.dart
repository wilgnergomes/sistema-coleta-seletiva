import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../app/sessao.dart';
import '../../app/tema/tokens.dart';
import '../../core/api/cliente_api.dart';
import '../../core/api/falhas.dart';
import '../../ui/compartilhado/campos.dart';
import '../../ui/compartilhado/envio.dart';
import '../../ui/compartilhado/estados.dart';
import '../../ui/compartilhado/layout.dart';
import '../moradores/data/moradores.dart';

/// Valida um ator com a primeira chamada do perfil antes de selecioná-lo.
class ValidadorAtor {
  ValidadorAtor(this._api, this._moradores);

  final ClienteApi _api;
  final RepositorioMoradores _moradores;

  /// Devolve o ator validado (com nome, quando a API o fornece) e a
  /// resposta da chamada, para reaproveitamento.
  Future<(Ator, Object?)> validar(Perfil perfil, int? id) async {
    final cabecalhos = Ator(perfil, id).cabecalhos;
    switch (perfil) {
      case Perfil.morador:
        final morador = await _moradores.obter(id!, identidade: cabecalhos);
        if (!morador.ativo) {
          throw const ErroHttp(status: 403, codigo: 'ACESSO_NEGADO', mensagem: 'Este morador está inativo.');
        }
        return (Ator(perfil, id, nome: morador.nome), morador);
      case Perfil.operador:
        final fila = await _api.get(
          '/solicitacoes',
          query: {'situacao': 'SOLICITADA', 'page': 0, 'size': 20},
          acesso: Acesso.ator,
          identidade: cabecalhos,
        );
        return (Ator(perfil, id), fila);
      case Perfil.administrador:
        final resumo = await _api.get('/resumo-gerencial', acesso: Acesso.ator, identidade: cabecalhos);
        return (Ator(perfil, null), resumo);
    }
  }
}

/// `/demonstracao`: escolha da identidade simulada (ADR 0019, RNF06).
class PaginaDemonstracao extends StatefulWidget {
  const PaginaDemonstracao({
    super.key,
    required this.sessao,
    required this.validador,
    required this.aoConcluir,
    required this.aoVoltarAoPublico,
    this.perfilInicial,
  });

  final Sessao sessao;
  final ValidadorAtor validador;

  /// Recebe o ator validado e a resposta da validação.
  final void Function(Ator ator, Object? resposta) aoConcluir;
  final VoidCallback aoVoltarAoPublico;
  final Perfil? perfilInicial;

  @override
  State<PaginaDemonstracao> createState() => _PaginaDemonstracaoState();
}

class _PaginaDemonstracaoState extends State<PaginaDemonstracao> {
  final _form = GlobalKey<FormState>();
  late Perfil _perfil = widget.perfilInicial ?? widget.sessao.perfil ?? Perfil.morador;
  final _id = TextEditingController();
  final _envio = EstadoEnvio();

  @override
  void dispose() {
    _id.dispose();
    _envio.dispose();
    super.dispose();
  }

  Future<void> _usar() async {
    if (!_form.currentState!.validate()) return;
    final id = _perfil.exigeId ? int.parse(_id.text.trim()) : null;
    final resultado = await _envio.executar(() => widget.validador.validar(_perfil, id));
    if (resultado != null) widget.aoConcluir(resultado.$1, resultado.$2);
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final atual = widget.sessao.ator;
    return PaginaRolavel(
      larguraMaxima: Largura.formulario,
      filhos: [
        const TituloPagina(
          'Identidade de demonstração',
          subtitulo: 'Simulação de identidade para apresentar as áreas restritas com dados fictícios.',
        ),
        const Aviso(
          tipo: TipoAviso.alerta,
          titulo: 'Simulação de identidade',
          mensagem:
              'Não há login real. O perfil e o identificador escolhidos são enviados à API por cabeçalhos '
              '(X-Perfil e X-Usuario-Id). Use somente em ambiente controlado e com dados fictícios.',
        ),
        if (atual != null) ...[
          const SizedBox(height: Espaco.m),
          Card(
            child: ListTile(
              leading: const Icon(Icons.person_pin_outlined),
              title: const Text('Contexto ativo'),
              subtitle: Text(atual.descricao),
              trailing: TextButton(
                onPressed: () {
                  widget.sessao.sair();
                  widget.aoVoltarAoPublico();
                },
                child: const Text('Sair do contexto'),
              ),
            ),
          ),
        ],
        const SizedBox(height: Espaco.l),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(Espaco.l),
            child: Form(
              key: _form,
              child: ListenableBuilder(
                listenable: _envio,
                builder: (context, _) => Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('Perfil', style: texto.titleMedium),
                    const SizedBox(height: Espaco.s),
                    RadioGroup<Perfil>(
                      groupValue: _perfil,
                      onChanged: (p) => setState(() {
                        _perfil = p ?? _perfil;
                        _envio.limpar();
                      }),
                      child: Column(
                        children: [
                          for (final p in Perfil.values)
                            RadioListTile<Perfil>(
                              value: p,
                              title: Text(p.rotulo),
                              subtitle: Text(switch (p) {
                                Perfil.morador => 'Consulta pontos, cria e acompanha as próprias solicitações.',
                                Perfil.operador => 'Atende a fila do ponto de coleta ao qual está vinculado.',
                                Perfil.administrador => 'Consulta o resumo e administra os cadastros.',
                              }),
                              contentPadding: EdgeInsets.zero,
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(height: Espaco.m),
                    MensagemEnvio(estado: _envio),
                    if (_perfil.exigeId)
                      CampoTexto(
                        controlador: _id,
                        rotulo: 'Identificador do ${_perfil.rotulo.toLowerCase()}',
                        obrigatorio: true,
                        ajuda: 'Número fornecido pelo roteiro da demonstração.',
                        teclado: TextInputType.number,
                        // Quem chega com o perfil escolhido digita o número e confirma com Enter.
                        autofocus: widget.perfilInicial != null,
                        formatadores: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(10)],
                        aoEnviar: (_) => _usar(),
                        validar: (t) {
                          final n = int.tryParse(t);
                          if (n == null || n < 1 || n > 2147483647) return 'Informe um número inteiro positivo.';
                          return null;
                        },
                      ),
                    Wrap(
                      alignment: WrapAlignment.end,
                      spacing: Espaco.s,
                      runSpacing: Espaco.s,
                      children: [
                        TextButton(onPressed: widget.aoVoltarAoPublico, child: const Text('Voltar à consulta pública')),
                        BotaoEnvio(
                          rotulo: 'Usar este ator',
                          enviando: _envio.enviando,
                          aoPressionar: _usar,
                          icone: Icons.login,
                          autofocus: widget.perfilInicial != null && !_perfil.exigeId,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
