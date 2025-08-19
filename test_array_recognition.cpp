#include <iostream>

const int MAXN = 100005;

bool vis[MAXN];  // Global array variable - should be recognized when typing "v"

int main() {
    int arr[10];    // Local array variable  
    char str[256];  // Another array variable
    
    for(int i = 0; i < 10; i++) {
        vis[i] = true;
        arr[i] = i;
    }
    
    return 0;
}