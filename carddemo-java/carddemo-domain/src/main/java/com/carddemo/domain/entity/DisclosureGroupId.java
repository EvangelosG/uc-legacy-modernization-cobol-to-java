package com.carddemo.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisclosureGroupId implements Serializable {

    private String groupId;
    private String tranTypeCd;
    private Integer tranCatCd;
}
