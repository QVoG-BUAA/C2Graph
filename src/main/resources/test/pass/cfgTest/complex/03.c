#include <bits/stdc++.h>

#define inf 0x3f
#define LLINF 1e18
#define INF 0x3f3f3f3f

using namespace std;

void dfs(int n) {
    if (!n) {
        cout << 0;
        return;
    }
    if (n == 1) {
        cout << "2(0)";
        return;
    }
    if (n == 2) {
        cout << 2;
        return;
    }


    bool flag = false;
    for (int i = 30; i >= 0; i--) {
        if (n & (1 << i)) {
            if (flag) {
                cout << "+";
            }
            flag = true;

            if (i == 1) {
                cout << 2;
                continue;
            }

            cout << "2(";
            dfs(i);
            cout << ")";
        }
    }
    int a;
    int b;
}

void solve() {
    cin >> n;
    dfs(n);
}

signed main() {
    cin.tie(0);
    cout.tie(0);
    ios::sync_with_stdio(false);

    int T = 1;
    while (T--) {
        solve();
    }

    return 0;
}