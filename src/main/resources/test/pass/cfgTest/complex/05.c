#include <bits/stdc++.h>

#define inf 0x3f
#define LLINF 1e18
#define INF 0x3f3f3f3f

using namespace std;

typedef long long ll;
typedef pair<int, char> PII;

/*
    以 (i, j) 这点做高
*/

char check(int l, int r) {
    bool all_0 = true;
    bool all_1 = true;
    for (int i = l; i <= r; i ++) {
        if (s[i] == '1') {
            all_0 = false;
        } else if (s[i] == '0') {
            all_1 = false;
        }
        if (!all_0 && !all_1) {
            break;
        }
    }
    if (all_0) {
        return 'B';
    }
    else if (all_1) {
        return 'I';
    }
    return 'F';
}

int build(int l, int r) {
    if (l > r) {
        return 0;
    }

    int now = ++tot;
    tr[now].c = check(l, r);
    if (l != r) {
        tr[now].l = build(l, l + r >> 1);
    }
    if (l != r) {
        tr[now].r = build((l + r + 1 >> 1) , r);
    }
    return now;
}

void dfs(int t) {
    if (!t) {
        return;
    }
    dfs(tr[t].l);
    dfs(tr[t].r);
    cout << tr[t].c;
}

void build(int prel, int prer, int posl, int posr) {
    if (prel > prer || posl > posr) {
        return;
    }
    for (int i = prel; i <= prer; i ++) {
        if (pre[i] == pos[posr]) {
            cout << pre[i];
            build(prel, i - 1, posl, posl + i - 1 - prel);
            build(i + 1, prer, posl + i - 1 - prel + 1, posr - 1);
            return;
        }
    }
}

void solve()
{
    cin >> pre >> pos;
    int n = pre.length();
    build(0, n - 1, 0, n - 1);
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