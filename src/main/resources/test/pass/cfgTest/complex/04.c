#include <bits/stdc++.h>

#define inf 0x3f
#define LLINF 1e18
#define INF 0x3f3f3f3f

using namespace std;

/*

*/
void solve()
{
    vector<int> b(n);

    int ans = 0;
    for (int i = 0; i < n; i ++) {
        int l = 0, r = ans;
        while (l < r) {
            int mid = l + r + 1 >> 1;
            if (b[mid] <= a[i]) {
                l = mid;
            }
            else {
                r = mid - 1;
            }
        }
        ans = max(ans, r + 1);
        b[r + 1] = a[i];
    }
}

signed main()
{
    cin.tie(0);
    cout.tie(0);
    ios::sync_with_stdio(false);

    int T = 1;
    while (T--) {
        solve();
    }

    return 0;
}