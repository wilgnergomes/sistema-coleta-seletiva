package br.edu.ifs.coletaseletiva.entity;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Registro de uma alteração da solicitação (RN03). Exatamente um responsável: operador OU morador
 * (ADR 0011, conferido pelo banco em ck_his_responsavel).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "his_historico")
public class Historico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "his_nr_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sol_nr_id", nullable = false)
    private Solicitacao solicitacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "his_tx_situacao_anterior", length = 20)
    private SituacaoSolicitacao situacaoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "his_tx_situacao_nova", nullable = false, length = 20)
    private SituacaoSolicitacao situacaoNova;

    @Column(name = "his_dt_data", nullable = false)
    private OffsetDateTime data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ope_nr_id")
    private Operador operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mor_nr_id")
    private Morador morador;

    @Column(name = "his_tx_motivo", length = 500)
    private String motivo;

    @Column(name = "his_dt_agendamento")
    private OffsetDateTime agendamento;

    @Column(name = "his_tx_informacoes_tentativa", length = 500)
    private String informacoesTentativa;
}
