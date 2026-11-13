package br.edu.ifs.coletaseletiva.entity;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import jakarta.persistence.*;
import lombok.*;

/** Responsável pelo recolhimento. Pertence a um único ponto de coleta (ADR 0010). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ope_operador")
public class Operador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ope_nr_id")
    private Integer id;

    @Column(name = "ope_tx_nome", nullable = false, length = 80)
    private String nome;

    /** Guardado normalizado (minúsculas, sem espaços), como exige ck_ope_email_normalizado. */
    @Column(name = "ope_tx_email", nullable = false, length = 150)
    private String email;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ope_tx_situacao", nullable = false, length = 10)
    private SituacaoCadastro situacao = SituacaoCadastro.ATIVO;

    /** Lado "muitos" da relação 1:N: vários operadores podem ter o mesmo ponto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poc_nr_id", nullable = false)
    private PontoColeta ponto;
}
