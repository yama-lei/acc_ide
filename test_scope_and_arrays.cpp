const int MAXN = 100005;
bool vis[MAXN];    // Global array - should be visible everywhere

void f() {
    int f1, f2, f3;  // Local variables in function f - should NOT be visible in main
    int arr[10];     // Local array - should NOT be visible in main
}

class mylist {
    int data;
    mylist *next;
};

int main() {
    vector<int> a;   // Local variable in main
    int x1, x2, x3 = 5;  // Local variables in main  
    
    // At this position (line 16), typing 'f' should show:
    // - f (function) - visible
    // - vis (global array) - visible  
    // - x1, x2, x3, a (local variables in main) - visible
    // But should NOT show f1, f2, f3, arr from function f()
    
    return 0;
}