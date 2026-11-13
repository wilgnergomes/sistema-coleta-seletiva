package br.edu.ifs.coletaseletiva.entity;

import jakarta.persistence.*;
import lombok.*;

/** Endereço de um morador. Cada endereço tem um único proprietário (RF02). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "end_endereco")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "end_nr_id")
    private Integer id;

    /** Lado "muitos" da relação: um morador tem vários endereços. LAZY: o morador só é carregado se for usado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mor_nr_id", nullable = false)
    private Morador morador;

    @Column(name = "end_tx_logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "end_tx_numero", nullable = false, length = 10)
    private String numero;

    @Column(name = "end_tx_bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "end_tx_municipio", nullable = false, length = 100)
    private String municipio;

    @Column(name = "end_tx_uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "end_tx_cep", nullable = false, length = 8)
    private String cep;

    @Column(name = "end_tx_complemento", length = 100)
    private String complemento;
}
