package br.edu.ifs.coletaseletiva.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Endereço do ponto de coleta (ADR 0005). {@code @Embeddable} é uma "peça" sem tabela nem id próprios:
 * seus campos viram colunas da tabela da entidade que a embute (aqui, poc_ponto_coleta).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EnderecoPonto {

    @Column(name = "poc_tx_logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "poc_tx_numero", nullable = false, length = 10)
    private String numero;

    @Column(name = "poc_tx_bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "poc_tx_municipio", nullable = false, length = 100)
    private String municipio;

    @Column(name = "poc_tx_uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "poc_tx_cep", nullable = false, length = 8)
    private String cep;

    @Column(name = "poc_tx_complemento", length = 100)
    private String complemento;
}
