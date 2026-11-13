/// Cache em memória limitado por quantidade de entradas e por idade.
class CacheMemoria<K, V> {
  CacheMemoria({this.maximo = 30, this.validade = const Duration(minutes: 2), DateTime Function()? relogio})
    : _relogio = relogio ?? DateTime.now;

  final int maximo;
  final Duration validade;
  final DateTime Function() _relogio;
  final _entradas = <K, (DateTime, V)>{};

  V? ler(K chave) {
    final entrada = _entradas.remove(chave);
    if (entrada == null) return null;
    if (_relogio().difference(entrada.$1) > validade) return null;
    _entradas[chave] = entrada; // Reinsere como a mais recente.
    return entrada.$2;
  }

  void gravar(K chave, V valor) {
    _entradas.remove(chave);
    _entradas[chave] = (_relogio(), valor);
    while (_entradas.length > maximo) {
      _entradas.remove(_entradas.keys.first);
    }
  }

  void remover(K chave) => _entradas.remove(chave);

  void limpar() => _entradas.clear();
}
