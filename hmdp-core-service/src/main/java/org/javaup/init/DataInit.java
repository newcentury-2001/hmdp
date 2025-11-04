package org.javaup.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.javaup.entity.SeckillVoucher;
import org.javaup.entity.Shop;
import org.javaup.handler.BloomFilterHandlerFactory;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IShopService;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_SHOP;
import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;

@Component
public class DataInit {
    
    @Resource
    private IShopService shopService;
    
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    
    @Resource
    private BloomFilterHandlerFactory bloomFilterHandlerFactory;

    @PostConstruct
    public void init() {
        List<Shop> shopList = shopService.list();
        for (Shop shop : shopList) {
            bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_SHOP).add(String.valueOf(shop.getId()));
        }
        List<SeckillVoucher> seckillVoucherlist = seckillVoucherService.list();
        for (SeckillVoucher seckillVoucher : seckillVoucherlist) {
            bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).add(String.valueOf(seckillVoucher.getVoucherId()));
        }
    }
}
