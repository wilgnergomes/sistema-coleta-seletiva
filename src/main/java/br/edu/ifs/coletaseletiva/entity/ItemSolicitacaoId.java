package br.edu.ifs.coletaseletiva.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Chave composta (solicitação, material): o mesmo material não aparece duas vezes na solicitação. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ItemSolicitacaoId implements Serializable {

    @Column(name = "sol_nr_id")
    private Integer solicitacaoId;

    @Column(name = "mat_nr_id")
    private Integer materialId;
}
