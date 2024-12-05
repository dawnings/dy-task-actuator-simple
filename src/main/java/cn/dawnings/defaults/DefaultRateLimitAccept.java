package cn.dawnings.defaults;

import cn.dawnings.coustoms.RateLimitAcceptInterface;

public class DefaultRateLimitAccept implements RateLimitAcceptInterface {
    @Override
    public boolean accept(String key) {
        return true;
    }
}
