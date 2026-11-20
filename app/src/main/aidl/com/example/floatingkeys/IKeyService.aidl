package com.example.floatingkeys;
interface IKeyService {
    void key(int keyCode, boolean down);
    void destroy() = 16777114;
}
