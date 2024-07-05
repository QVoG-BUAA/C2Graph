#include <stdio.h>

int main ()
{
   switch(grade)
   {
   case 'A' :
      printf("很棒！\n" );
      break;
   case 'B' :
   case 'C' :
      printf("做得好\n" );
      break;
   case 'D' :
      printf("您通过了\n" );
      break;
   case 'F' :
      printf("最好再试一下\n" );
      for (int i = 0; i < 5; i ++) {
        cout << i << endl;
        cout << i + 1 << endl;
      }
      break;
   default :
      for (int i2 = 0; i2 < 5; i2 ++) {
        cout << i2 << endl;
        cout << i2 + 1 << endl;
      }
   }
   char grade = 'B';

   return 0;
}