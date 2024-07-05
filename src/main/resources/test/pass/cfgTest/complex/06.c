//bucket
#include<iostream>
#define max 1000
using namespace std;

int main()
{
    int n;
    cin>>n;

    int temp;
    int num[max]={0};
    for(int i=0;i<n;i++){
        cin>>temp;
        num[temp]++;
    }

    int cnt=0;
    for(int j=0;j<max;j++){
        if(num[j]!=0){
            cnt++;
        }
    }
    cout<<cnt<<endl;
    for(int k=0;k<max;k++){
        if(num[k]!=0){
            cout<<k<<' ';
        }
    }
    return 0;
}