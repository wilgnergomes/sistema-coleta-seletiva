import 'dart:async';

import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/cliente_api.dart';
import '../../../core/api/falhas.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../data/modelos.dart';
import '../data/repositorio_catalogo.dart';
import 'componentes_catalogo.dart';
import '../../../ui/compartilhado/superficie.dart';

/// Disponibilidade de um material vinculado, confirmada pelo catálogo.
enum Disponibilidade { disponivel, indisponivel, verificando, desconhecida }

class DetalhePontoViewModel extends ChangeNotifier {
  DetalhePontoViewModel(this._repositorio, this.id, {this.acesso = Acesso.publico});

  final RepositorioCatalogo _repositorio;
  final int id;
  final Acesso acesso;
  final ponto = Carga<PontoColeta>();
  final disponibilidade = <int, Disponibilidade>{};

  Future<void> carregar({bool forcar = false}) async {
    await ponto.executar(() => _repositorio.ponto(id, acesso: acesso, forcar: forcar), notifyListeners);
    final p = ponto.dados;
    if (p != null && acesso == Acesso.publico) await _confirmarMateriais(p);
  }

  /// Usa a página de ativos já em memória. Só consulta o material
  /// individualmente quando a página não é completa e ele não apareceu.
  Future<void> _confirmarMateriais(PontoColeta p) async {
    CatalogoMateriais? catalogo;
    try {
      catalogo = await _repositorio.materiaisAtivos();
    } on FalhaApi {
      catalogo = null;
    }
    for (final vinculo in p.materiaisAceitos) {
      final conhecido = catalogo?.porId(vinculo.materialId);
      if (conhecido != null) {
        disponibilidade[vinculo.materialId] = Disponibilidade.disponivel;
      } else if (catalogo != null && catalogo.completo) {
        disponibilidade[vinculo.materialId] = Disponibilidade.indisponivel;
      } else {
        disponibilidade[vinculo.materialId] = Disponibilidade.verificando;
        unawaited(_confirmarIndividual(vinculo.materialId));
      }
    }
    notifyListeners();
  }

  Future<void> _confirmarIndividual(int materialId) async {
    try {
      final m = await _repositorio.material(materialId);
      disponibilidade[materialId] = m.ativo ? Disponibilidade.disponivel : Disponibilidade.indisponivel;
    } on ErroHttp catch (e) {
      disponibilidade[materialId] = e.naoEncontrado ? Disponibilidade.indisponivel : Disponibilidade.desconhecida;
    } on FalhaApi {
      disponibilidade[materialId] = Disponibilidade.desconhecida;
    }
    notifyListeners();
  }
}

/// Detalhe do ponto: endereço, contato, horários, modalidade e materiais.
class PaginaDetalhePonto extends StatefulWidget {
  const PaginaDetalhePonto({
    super.key,
    required this.id,
    required this.repositorio,
    required this.aoVoltar,
    this.acesso = Acesso.publico,
    this.aoAbrirMaterial,
    this.acoes,
    this.secaoExtra,
  });

  final int id;
  final RepositorioCatalogo repositorio;
  final VoidCallback aoVoltar;
  final Acesso acesso;
  final ValueChanged<int>? aoAbrirMaterial;

  /// Ações do contexto, como `Solicitar recolhimento` para o morador.
  final Widget Function(BuildContext, PontoColeta)? acoes;

  /// Conteúdo adicional, como a gestão de vínculos do administrador.
  final Widget Function(BuildContext, PontoColeta, VoidCallback recarregar)? secaoExtra;

  @override
  State<PaginaDetalhePonto> createState() => _PaginaDetalhePontoState();
}

class _PaginaDetalhePontoState extends State<PaginaDetalhePonto> {
  late final _vm = DetalhePontoViewModel(widget.repositorio, widget.id, acesso: widget.acesso);

  @override
  void initState() {
    super.initState();
    _vm.carregar();
  }

  @override
  void dispose() {
    _vm.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: _vm,
      builder: (context, _) {
        final falha = _vm.ponto.falha;
        if (falha is ErroHttp && falha.naoEncontrado) {
          return PaginaRolavel(
            larguraMaxima: Largura.formulario,
            filhos: [
              EstadoVazio(
                icone: Icons.location_off_outlined,
                titulo: 'Ponto de coleta indisponível',
                mensagem: 'Este ponto não foi encontrado ou não está ativo no catálogo.',
                acao: Elevavel(
                  child: FilledButton(onPressed: widget.aoVoltar, child: const Text('Voltar à pesquisa')),
                ),
              ),
            ],
          );
        }
        return PaginaRolavel(
          larguraMaxima: Largura.densa,
          filhos: [
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: widget.aoVoltar,
                icon: const Icon(Icons.arrow_back),
                label: const Text('Voltar'),
              ),
            ),
            const SizedBox(height: Espaco.xs),
            ConteudoCarga<PontoColeta>(
              carga: _vm.ponto,
              rotuloCarregando: 'Carregando ponto de coleta…',
              aoTentarNovamente: () => _vm.carregar(forcar: true),
              construir: _conteudo,
            ),
          ],
        );
      },
    );
  }

  Widget _conteudo(BuildContext context, PontoColeta p) {
    final texto = Theme.of(context).textTheme;
    final dados = Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TituloPagina(p.nome),
        Wrap(
          spacing: Espaco.xs,
          runSpacing: Espaco.xs,
          children: [
            SeloModalidade(domiciliar: p.recolhimentoDomiciliar),
            if (!p.ativo) const SeloInativo(),
          ],
        ),
        if (widget.acoes != null) ...[const SizedBox(height: Espaco.m), widget.acoes!(context, p)],
        const TituloSecao('Endereço'),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(Espaco.m),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(p.endereco.linha1, style: texto.bodyLarge),
                Text(p.endereco.linha2, style: texto.bodyMedium?.copyWith(color: Cores.textoSecundario)),
              ],
            ),
          ),
        ),
        const TituloSecao('Atendimento'),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(Espaco.m),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                CampoLeitura(rotulo: 'Horários', valor: _ouPadrao(p.horarios, 'Horários não informados')),
                CampoLeitura(rotulo: 'Contato', valor: _ouPadrao(p.contato, 'Contato não informado')),
                CampoLeitura(
                  rotulo: 'Modalidade',
                  valor: p.recolhimentoDomiciliar
                      ? 'Faz recolhimento domiciliar. A data é combinada pelo ponto após a solicitação.'
                      : 'Recebe os materiais no próprio local. Não faz recolhimento domiciliar.',
                ),
              ],
            ),
          ),
        ),
      ],
    );
    final materiais = Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const TituloSecao('Materiais aceitos'),
        if (p.materiaisAceitos.isEmpty)
          const EstadoVazio(icone: Icons.recycling, titulo: 'Nenhum material informado neste ponto')
        else
          for (final m in p.materiaisAceitos) ...[
            _CartaoVinculo(
              vinculo: m,
              disponibilidade: widget.acesso == Acesso.publico ? _vm.disponibilidade[m.materialId] : null,
              aoAbrir: widget.aoAbrirMaterial == null ? null : () => widget.aoAbrirMaterial!(m.materialId),
            ),
            const SizedBox(height: Espaco.s),
          ],
      ],
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        LayoutBuilder(
          builder: (context, restricoes) {
            if (restricoes.maxWidth < 900) {
              return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [dados, materiais]);
            }
            return Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(child: dados),
                const SizedBox(width: Espaco.l),
                Expanded(
                  child: Padding(padding: const EdgeInsets.only(top: 76), child: materiais),
                ),
              ],
            );
          },
        ),
        if (widget.secaoExtra != null) widget.secaoExtra!(context, p, () => _vm.carregar(forcar: true)),
      ],
    );
  }

  static String _ouPadrao(String? valor, String padrao) => valor == null || valor.trim().isEmpty ? padrao : valor;
}

class SeloInativo extends StatelessWidget {
  const SeloInativo({super.key});

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: Espaco.xs, vertical: Espaco.xxs),
    decoration: BoxDecoration(color: Cores.neutroFundo, borderRadius: BorderRadius.circular(Raio.campo)),
    child: Text('Inativo', style: Theme.of(context).textTheme.labelMedium?.copyWith(color: Cores.neutro)),
  );
}

class _CartaoVinculo extends StatelessWidget {
  const _CartaoVinculo({required this.vinculo, required this.disponibilidade, this.aoAbrir});

  final MaterialAceito vinculo;
  final Disponibilidade? disponibilidade;
  final VoidCallback? aoAbrir;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final indisponivel = disponibilidade == Disponibilidade.indisponivel;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(Espaco.m),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            IconeMaterial(nome: vinculo.nomeMaterial),
            const SizedBox(width: Espaco.s),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: Espaco.xs,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Text(vinculo.nomeMaterial, style: texto.titleMedium),
                      Text('· medido em ${vinculo.unidade.extenso}', style: texto.bodySmall),
                      if (indisponivel)
                        Text(
                          '· indisponível no catálogo',
                          style: texto.bodySmall?.copyWith(color: Cores.alerta, fontWeight: FontWeight.w600),
                        ),
                    ],
                  ),
                  if (vinculo.orientacao.isNotEmpty) ...[
                    const SizedBox(height: Espaco.xxs),
                    Text(vinculo.orientacao, style: texto.bodyMedium),
                  ],
                  if (vinculo.observacoes != null && vinculo.observacoes!.trim().isNotEmpty) ...[
                    const SizedBox(height: Espaco.xxs),
                    Text('Observação do ponto: ${vinculo.observacoes}', style: texto.bodyMedium),
                  ],
                  const SizedBox(height: Espaco.xxs),
                  Text(
                    vinculo.limite == null
                        ? 'Sem limite por solicitação'
                        : 'Limite por solicitação: ${vinculo.limite!.comUnidade(vinculo.unidade)}',
                    style: texto.bodySmall,
                  ),
                  if (aoAbrir != null && !indisponivel)
                    TextButton(
                      onPressed: aoAbrir,
                      child: Text('Ver orientações', semanticsLabel: 'Ver orientações de ${vinculo.nomeMaterial}'),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
