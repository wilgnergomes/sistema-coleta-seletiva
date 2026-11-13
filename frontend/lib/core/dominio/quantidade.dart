import 'enums.dart';

/// Quantidade decimal com até 3 casas, guardada em milésimos inteiros.
///
/// O contrato aceita até 9 dígitos inteiros e 3 decimais
/// (`999999999.999`), o que cabe com folga no inteiro seguro da Web
/// (2^53). Assim a validação e a serialização não dependem do arredondamento
/// binário de `double`.
class Quantidade implements Comparable<Quantidade> {
  const Quantidade.milesimos(this.milesimos);

  static const zero = Quantidade.milesimos(0);
  static const maximo = Quantidade.milesimos(999999999999);

  final int milesimos;

  /// Converte o número recebido no JSON.
  factory Quantidade.deNumero(num valor) => Quantidade.milesimos((valor * 1000).round());

  bool get positiva => milesimos > 0;
  bool get inteira => milesimos % 1000 == 0;

  /// Número para o JSON, como `1.25` ou `2`.
  num get comoNumero => inteira ? milesimos ~/ 1000 : milesimos / 1000;

  @override
  int compareTo(Quantidade outra) => milesimos.compareTo(outra.milesimos);

  @override
  bool operator ==(Object other) => other is Quantidade && other.milesimos == milesimos;

  @override
  int get hashCode => milesimos.hashCode;

  /// Texto pt-BR, sem zeros finais: `1,25`, `12,5`, `4`.
  String get texto {
    final inteiro = milesimos ~/ 1000;
    final fracao = milesimos % 1000;
    final parteInteira = _agrupar(inteiro);
    if (fracao == 0) return parteInteira;
    final decimais = fracao.toString().padLeft(3, '0').replaceFirst(RegExp(r'0+$'), '');
    return '$parteInteira,$decimais';
  }

  /// Texto com unidade: `12,5 kg`, `3 un.`.
  String comUnidade(Unidade unidade) => unidade.simbolo.isEmpty ? texto : '$texto ${unidade.simbolo}';

  static String _agrupar(int valor) {
    final digitos = valor.toString();
    final grupos = <String>[];
    for (var fim = digitos.length; fim > 0; fim -= 3) {
      grupos.insert(0, digitos.substring(fim - 3 < 0 ? 0 : fim - 3, fim));
    }
    return grupos.join('.');
  }

  @override
  String toString() => texto;
}

/// Resultado da leitura de uma quantidade digitada.
class LeituraQuantidade {
  const LeituraQuantidade._(this.valor, this.erro);

  final Quantidade? valor;
  final String? erro;

  /// Interpreta texto digitado em pt-BR.
  ///
  /// Aceita vírgula ou ponto como separador decimal e no máximo 3 casas.
  /// Não aceita separador de milhar, para evitar ambiguidade entre `1.234`
  /// (um vírgula dois três quatro) e mil duzentos e trinta e quatro.
  factory LeituraQuantidade.de(String texto, {required Unidade unidade, bool permitirZero = false}) {
    final limpo = texto.trim().replaceAll(' ', '');
    if (limpo.isEmpty) return const LeituraQuantidade._(null, 'Informe a quantidade.');
    final casa = RegExp(r'^(\d{1,9})(?:[.,](\d{1,3}))?$').firstMatch(limpo);
    if (casa == null) {
      if (RegExp(r'^\d+([.,]\d+)?$').hasMatch(limpo)) {
        return const LeituraQuantidade._(null, 'Use até 9 dígitos inteiros e 3 casas decimais.');
      }
      return const LeituraQuantidade._(null, 'Use apenas números, com vírgula para decimais.');
    }
    final inteiro = int.parse(casa.group(1)!);
    final fracao = int.parse((casa.group(2) ?? '').padRight(3, '0'));
    final valor = Quantidade.milesimos(inteiro * 1000 + fracao);
    if (!permitirZero && !valor.positiva) {
      return const LeituraQuantidade._(null, 'A quantidade deve ser maior que zero.');
    }
    if (unidade.inteira && !valor.inteira) {
      return const LeituraQuantidade._(null, 'Este material é contado em unidades inteiras.');
    }
    return LeituraQuantidade._(valor, null);
  }

  bool get valida => valor != null;
}
