import 'package:timezone/data/latest_10y.dart' deferred as base_fusos;
import 'package:timezone/timezone.dart' as tz;

/// Datas no fuso de negócio (ADR 0014), o mesmo do backend.
///
/// A base IANA é carregada sob demanda (importação adiada), no primeiro uso
/// de datas de negócio. O catálogo público não depende dela.
class FusoNegocio {
  FusoNegocio(this.nome);

  final String nome;
  tz.Location? _local;
  Future<void>? _carga;

  bool get pronto => _local != null;

  Future<void> garantir() => _carga ??= () async {
    await base_fusos.loadLibrary();
    base_fusos.initializeTimeZones();
    _local = tz.getLocation(nome);
  }();

  tz.Location get _localizacao {
    final local = _local;
    if (local == null) throw StateError('Base de fusos não carregada: chame garantir() antes.');
    return local;
  }

  /// Converte um instante para a hora de parede do fuso de negócio.
  tz.TZDateTime local(DateTime instante) => tz.TZDateTime.from(instante, _localizacao);

  /// Combina data e hora de parede no fuso de negócio e devolve o instante.
  /// Lança [ArgumentError] para horário inexistente ou ambíguo na zona.
  DateTime instante(DataCivil dia, int horaDoDia, int minuto) {
    final convertido = tz.TZDateTime(_localizacao, dia.ano, dia.mes, dia.dia, horaDoDia, minuto);
    if (convertido.year != dia.ano ||
        convertido.month != dia.mes ||
        convertido.day != dia.dia ||
        convertido.hour != horaDoDia ||
        convertido.minute != minuto) {
      throw ArgumentError('Horário inexistente neste fuso. Escolha outro horário.');
    }
    bool mesmaHora(int deslocamento) {
      final vizinho = tz.TZDateTime.fromMillisecondsSinceEpoch(
        _localizacao,
        convertido.millisecondsSinceEpoch + deslocamento,
      );
      return vizinho.hour == convertido.hour && vizinho.minute == convertido.minute && vizinho.day == convertido.day;
    }

    const hora = 3600000;
    if (mesmaHora(hora) || mesmaHora(-hora)) {
      throw ArgumentError('Horário ambíguo neste fuso. Escolha outro horário.');
    }
    return convertido.toUtc();
  }

  /// Hoje no fuso de negócio.
  DataCivil hoje(DateTime agora) {
    final l = local(agora);
    return DataCivil(l.year, l.month, l.day);
  }

  /// `05/10/2026 09:00`.
  String dataHora(DateTime instante) {
    final l = local(instante);
    return '${_dois(l.day)}/${_dois(l.month)}/${l.year} ${_dois(l.hour)}:${_dois(l.minute)}';
  }

  /// `05/10/2026`.
  String data(DateTime instante) {
    final l = local(instante);
    return '${_dois(l.day)}/${_dois(l.month)}/${l.year}';
  }

  /// Abreviação do fuso para rotular datas, como `BRT` ou `-03`.
  String rotulo(DateTime instante) {
    final l = local(instante);
    final abreviacao = l.timeZoneName;
    if (abreviacao.isNotEmpty && !abreviacao.startsWith('-') && !abreviacao.startsWith('+')) return abreviacao;
    final h = l.timeZoneOffset.inMinutes;
    final sinal = h < 0 ? '-' : '+';
    return 'UTC$sinal${_dois(h.abs() ~/ 60)}${h.abs() % 60 == 0 ? '' : ':${_dois(h.abs() % 60)}'}';
  }
}

String _dois(int n) => n.toString().padLeft(2, '0');

/// Data civil (`AAAA-MM-DD`) dos filtros de período, sem fuso.
class DataCivil implements Comparable<DataCivil> {
  const DataCivil(this.ano, this.mes, this.dia);

  /// Lê `AAAA-MM-DD`, ou nulo se inválida.
  static DataCivil? ler(String? texto) {
    if (texto == null) return null;
    final m = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(texto.trim());
    if (m == null) return null;
    final ano = int.parse(m.group(1)!);
    final mes = int.parse(m.group(2)!);
    final dia = int.parse(m.group(3)!);
    final teste = DateTime.utc(ano, mes, dia);
    if (teste.year != ano || teste.month != mes || teste.day != dia) return null;
    return DataCivil(ano, mes, dia);
  }

  static DataCivil deDateTime(DateTime d) => DataCivil(d.year, d.month, d.day);

  final int ano;
  final int mes;
  final int dia;

  String get iso => '${ano.toString().padLeft(4, '0')}-${_dois(mes)}-${_dois(dia)}';
  String get exibicao => '${_dois(dia)}/${_dois(mes)}/$ano';
  DateTime get comoDateTime => DateTime(ano, mes, dia);

  @override
  int compareTo(DataCivil outra) => iso.compareTo(outra.iso);

  @override
  bool operator ==(Object other) => other is DataCivil && other.iso == iso;

  @override
  int get hashCode => iso.hashCode;

  @override
  String toString() => iso;
}
