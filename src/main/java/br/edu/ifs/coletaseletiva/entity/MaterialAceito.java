package br.edu.ifs.coletaseletiva.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Vínculo entre um ponto de coleta e um material que ele aceita (RF05). É a relação N:N entre ponto e
 * material com atributos próprios (observações e limite), por isso vira uma entidade, e não um simples
 * {@code @ManyToMany}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mac_material_aceito")
public class MaterialAceito {

    @EmbeddedId
    private MaterialAceitoId id;

    /** {@code @MapsId} diz que a coluna poc_nr_id da chave é a mesma desta relação: a chave vem do ponto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("pontoId")
    @JoinColumn(name = "poc_nr_id")
    private PontoColeta ponto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("materialId")
    @JoinColumn(name = "mat_nr_id")
    private Material material;

    @Column(name = "mac_tx_observacoes", length = 500)
    private String observacoes;

    /** Limite por solicitação, na unidade do material. Nulo = sem limite; quando informado, é maior que zero. */
    @Column(name = "mac_nr_limite", precision = 12, scale = 3)
    private BigDecimal limite;
}
