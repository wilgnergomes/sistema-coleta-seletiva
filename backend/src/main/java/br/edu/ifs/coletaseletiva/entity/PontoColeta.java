package br.edu.ifs.coletaseletiva.entity;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "poc_ponto_coleta")
public class PontoColeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poc_nr_id")
    private Integer id;

    @Column(name = "poc_tx_nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "poc_tx_contato", length = 150)
    private String contato;

    /** Texto descritivo (ADR 0006). */
    @Column(name = "poc_tx_horarios", length = 500)
    private String horarios;

    @Column(name = "poc_bl_rec_domiciliar", nullable = false)
    private boolean recolhimentoDomiciliar;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "poc_tx_situacao", nullable = false, length = 10)
    private SituacaoCadastro situacao = SituacaoCadastro.ATIVO;

    /** Objeto de valor embutido: as colunas poc_tx_logradouro... pertencem a esta mesma tabela. */
    @Embedded
    private EnderecoPonto endereco;
}
