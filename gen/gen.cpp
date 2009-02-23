#include <iostream>
#include <cstdlib>
#include <random>
#include <ctime>
#include <cassert>
#include <cmath>
#include <utility>
#include <algorithm>
#include "color.h"
#include <cstring>
#include <queue>

using namespace std;

ranlux_base_01 rndf(time(0));

const int MAX_SIZE = 4096;

float xs[1024], ys[1024];
int rxs[1024][1024], rys[1024][1024];
int amount;
int n;
int w,h;

float asd=.2;
float rndf2()
{
	return rndf()-.5f;
}

void genTriangle()
{
	float x=rndf(), y=rndf();
//	float a=1.5/(log(n)*log(n));
	float a = asd;
//	fprintf(stderr, "%f\n", a);
	for(int i=0; i<3; ++i) {
		xs[i] = x+a*rndf2();
		ys[i] = y+a*rndf2();
		++amount;
	}
}

typedef unsigned char ubyte;
ubyte arr[MAX_SIZE][MAX_SIZE];

void bound(int& a, int low, int hi)
{
	a = max(a,low);
	a = min(a,hi);
}

void fill(int x1, int y1, int x2, int y2, int x3, int y3)
{
	float dx2 = float(x2-x1)/(y2-y1);
	float dx3 = float(x3-x1)/(y3-y1);
	int end=min(y2,h);
	for(int i=max(y1,0); i<end; ++i) {
		int k=i-y1;
		int ax=x1+k*dx2, bx=x1+k*dx3;
		if (bx<ax) swap(ax,bx);
		bound(ax,0,w);
		bound(bx,0,w);

		for(int j=ax; j<bx; ++j)
			arr[i][j] = 1;
	}
	dx2 = float(x2-x3)/(y2-y3);
	end = min(y3,h);
	for(int i=max(y2,0); i<end; ++i) {
		int k=i-y1;
		int l=i-y2;
		int ax=x2+l*dx2, bx=x1+k*dx3;
		if (bx<ax) swap(ax,bx);
		bound(ax,0,w);
		bound(bx,0,w);

		for(int j=ax; j<bx; ++j)
			arr[i][j] = 1;
	}
}
struct Loc {
	Loc(){}
	Loc(int a, int b):x(a),y(b){}
	int x,y;
};
#define FAT 5
void fillx(int x1, int y1, int x2, int y2, int x3, int y3)
{
	int mx = (x1+x2+x3)/3;
	int my = (y1+y2+y3)/3;
	Loc a[3] = {Loc(x1,y1),Loc(x2,y2),Loc(x3,y3)};
	for(int i=0; i<3; ++i) {
		float dx=a[i].x-mx;
		float dy=a[i].y-my;
		float l = sqrt(dx*dx+dy*dy);
		dx/=l, dy/=l;
		a[i].x += dx*FAT;
		a[i].y += dy*FAT;
	}
	fill(a[0].x,a[0].y,a[1].x,a[1].y,a[2].x,a[2].y);
}

int dx[4] = {0,1,0,-1};
int dy[4] = {1,0,-1,0};

bool dfs(int x, int y, int d)
{
	if (!d) return 1;
	bool r=0;
	arr[y][x] = 1;
	for(int i=0; i<4; ++i) {
		int x2=x+dx[i], y2=y+dy[i];
		if (x2>=0 && y2>=0 && x2<w && y2<h && !arr[y2][x2])
			r |= dfs(x2,y2,d-1);
	}
	return r;
}
void bfs(int x, int y)
{
	arr[y][x]=1;
	queue<Loc> q;
	q.push(Loc(x,y));
	while(!q.empty()) {
		Loc l=q.front();
		q.pop();
		for(int i=0; i<4; ++i) {
			int x2=l.x+dx[i], y2=l.y+dy[i];
			if (x2>=0 && y2>=0 && x2<w && y2<h && !arr[y2][x2])
				arr[y2][x2]=1, q.push(Loc(x2,y2));
		}
	}
}

bool levelOK()
{
	memset(arr, 0, sizeof(arr));
	for(int i=0; i<n; ++i) {
		int *ax=rxs[i], *ay=rys[i];
		fillx(ax[0],ay[0],ax[1],ay[1],ax[2],ay[2]);
	}
	bool found=0;
	for(int i=0; i<h; ++i) {
		for(int j=0; j<w; ++j)
			if (!arr[i][j]) {
				if (found) return 0;
				found = 1;
				bfs(j,i);
//				found = !dfs(j,i,10000);
//				printf("asd: %d\n", found);
			}
	}
	return found;
}

int main(int argc, char* argv[])
{
#if 0
	w=h=60;
	fill(40, 1, -5, 30, 50, 70, 60, 60);
	for(int i=0; i<60; ++i) {
		for(int j=0; j<100; ++j)
			putchar(arr[i][j] ? 'x' : ' ');
		putchar(10);
	}
	return 0;
#endif

	assert(argc >= 4);
	w=atoi(argv[1]), h=atoi(argv[2]);
	assert(w<=MAX_SIZE && h<=MAX_SIZE);
	n = atoi(argv[3]);
	if (n>=5) asd=atof(argv[4]);

alku:
	fprintf(stderr, "asd\n");
	for(int i=0; i<n; ++i) {
		amount=0;
		genTriangle();
		for(int j=0; j<amount; ++j)
			rxs[i][j]=xs[j]*w, rys[i][j]=ys[j]*h;
	}
	if (!levelOK())
		goto alku;
	printf("%d %d %x\n", w,h,HSV(255*rndf(), 255, 255));
	for(int i=0; i<n; ++i) {
		printf("%x ", HSV(255*rndf(), 255, 255));
		for(int j=0; j<amount; ++j)
			printf("%d %d ", rxs[i][j], rys[i][j]);
		putchar(10);
	}
}
