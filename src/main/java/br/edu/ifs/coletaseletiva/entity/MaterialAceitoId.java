package br.edu.ifs.coletaseletiva.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Chave primária composta de mac_material_aceito: (ponto, material). Como é a chave, o mesmo vínculo
 * nunca se repete. Uma chave composta precisa de equals/hashCode e de ser Serializable.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class MaterialAceitoId implements Serializable {

    @Column(name = "poc_nr_id")
    private Integer pontoId;

    @Column(name = "mat_nr_id")
    private Integer materialId;
}
