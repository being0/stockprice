package com.being0.stockprice.thirdparty;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author <a href="mailto:raliakbari@gmail.com">Reza Aliakbari</a>
 * @version 1, 08/22/2020
 */
@Data
@AllArgsConstructor
public class Price {

    private String stock;
    private Double value;
}
