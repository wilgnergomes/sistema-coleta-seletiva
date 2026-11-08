package br.edu.ifs.coletaseletiva.entity;

import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "iso_item_solicitacao")
public class ItemSolicitacao {

    @EmbeddedId
    private ItemSolicitacaoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("solicitacaoId")
    @JoinColumn(name = "sol_nr_id")
    private Solicitacao solicitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("materialId")
    @JoinColumn(name = "mat_nr_id")
    private Material material;

    /** Unidade do material no momento do pedido (ADR 0008). */
    @Enumerated(EnumType.STRING)
    @Column(name = "iso_tx_copia_unidade", nullable = false, length = 5)
    private Unidade unidadeCopia;

    @Column(name = "iso_nr_quantidade_estimada", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeEstimada;

    @Column(name = "iso_nr_quantidade_efetiva", precision = 12, scale = 3)
    private BigDecimal quantidadeEfetiva;
}
