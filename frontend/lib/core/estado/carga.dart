import '../api/falhas.dart';

/// Estado de uma leitura assíncrona com descarte de respostas obsoletas.
///
/// Cada execução recebe uma geração. Se outra execução começar, ou a carga
/// for invalidada (troca de ator ou de filtro), a resposta antiga é ignorada.
class Carga<T> {
  T? dados;
  FalhaApi? falha;
  bool carregando = false;
  int _geracao = 0;

  bool get carregada => dados != null;

  /// Executa [buscar] e chama [notificar] ao começar e ao terminar.
  ///
  /// Com [limpar], descarta os dados anteriores (por exemplo, detalhe
  /// restrito de outro registro). Sem ele, o conteúdo anterior continua
  /// visível enquanto a atualização acontece.
  Future<void> executar(Future<T> Function() buscar, void Function() notificar, {bool limpar = false}) async {
    final geracao = ++_geracao;
    carregando = true;
    falha = null;
    if (limpar) dados = null;
    notificar();
    try {
      final resultado = await buscar();
      if (geracao != _geracao) return;
      dados = resultado;
    } on FalhaApi catch (erro) {
      if (geracao != _geracao) return;
      falha = erro;
    } finally {
      if (geracao == _geracao) {
        carregando = false;
        notificar();
      }
    }
  }

  /// Substitui os dados após uma mutação confirmada.
  void definir(T valor) {
    _geracao++;
    dados = valor;
    falha = null;
    carregando = false;
  }

  /// Esquece dados e respostas pendentes.
  void invalidar() {
    _geracao++;
    dados = null;
    falha = null;
    carregando = false;
  }
}
