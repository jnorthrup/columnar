package cursors.calendar


import cursors.*
import cursors.TypeMemento
import cursors.context.*
import cursors.io.*
import cursors.macros.join
import org.junit.jupiter.api.Test
import vec.macros.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField.DAY_OF_MONTH
import java.time.temporal.ChronoField.MONTH_OF_YEAR
import java.time.temporal.TemporalAdjusters


/**
 * small example code to blow out calendar dates
 */
@Suppress("UNCHECKED_CAST")
class CalendarTest {
    val coords = intArrayOf(
            0, 10,
            10, 84,
            84, 124,
            124, 164
    ).zipWithNext() //α { (a:Int,b:Int) :Pai2<Int,Int> -> Tw1n (a,b)   }

    val drivers = vect0rOf(
            IOMemento.IoLocalDate as TypeMemento,
            IOMemento.IoString,
            IOMemento.IoFloat,
            IOMemento.IoFloat
    )

    val names = vect0rOf("date", "channel", "delivered", "ret")
    val mf = MappedFile("src/test/resources/caven4.fwf")
    val nio = NioMMap(mf)
    val fixedWidth: FixedWidth
        get() = RowMajor.fixedWidthOf(nio = nio, coords = coords)

    @Suppress("UNCHECKED_CAST")
    val caven4Root = RowMajor().fromFwf(
            fixedWidth,
            RowMajor.indexableOf(nio, fixedWidth),
            nio,
            Columnar(drivers.zip(names) as Vect02<TypeMemento, String?>)
    )


    @Test
    fun test4Rows() {

        val caven4Cursor = cursorOf(caven4Root)

        println("---")

        val join1 = join(caven4Cursor[0], caven4Cursor[1, 2, 3])
        val scalars1 = join1.scalars as Vect02<TypeMemento, String?>
        join1.map { it.left.toList() }.toList().forEach(::println)



        JvmCal.values().forEach { jvmCal: JvmCal ->

            println("--- " + jvmCal)

            val csrc = caven4Cursor[0]
            val xSize = csrc.scalars.size
            val v: Cursor = Cursor(csrc.size) { iy: Int ->
                RowVec(xSize) { ix: Int ->
                    val row = csrc at (iy)
                    (row.left[ix] as? LocalDate)?.let { localDate: LocalDate ->
                        val localDate1 = localDate
                        val categories = jvmCal.dateWiseCategories(localDate1)
                        categories.toString() t2 {
                            val second = row[ix].second()
                            second + Scalar(IOMemento.IoString, "${jvmCal.name}_map")
                        }

                    } ?: row[ix]
                }
            }

            val join = join(v, caven4Cursor[1, 2, 3])

            val scalars = join.scalars as Vect02<TypeMemento, String?>
            println(scalars.right.toList())
            join.map { it.left.toList() }.toList().forEach(::println)
        }
    }


    @Test
    fun testHijRah() {
        //first day of Ramadan, 9th month
        val ramadan = HijrahDate.now().with(DAY_OF_MONTH, 1).with(MONTH_OF_YEAR, 9)
        println("HijrahDate : $ramadan")
        //HijrahDate -> LocalDate
        println("\n--- Ramandan 2016 ---")
        println("Start : " + LocalDate.from(ramadan))
        //until the end of the month
        println("End : " + LocalDate.from(ramadan.with(TemporalAdjusters.lastDayOfMonth())))
    }

    @Test
    fun dumpEm() {
        JvmCal.values().forEach { jvmCalOptions ->
            println(jvmCalOptions.info().toString())
        }
    }

    @Test
    fun TestJan1970() {
        val localDate = LocalDate.of(1970, 1, 1)

        val zonedDateTime =
                ZonedDateTime.ofInstant(localDate.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
        JvmCal.values().forEach { jvmCal: JvmCal ->
            System.err.println("$jvmCal")
            System.err.println("$jvmCal ${jvmCal.jvmProxy.date(zonedDateTime)}")
        }
    }
}

/**
function varargout = weton(year,month,day,n)
%WETON	Javanese calendar / Wetonan.
%	WETON without input argument returns the javanese date for today,
%	in the form:
%	   DINAPITU PASARAN WUKU DINA WULAN T TAUN WINDU KURUP, DAY MONTH YEAR (DINA MULYA)
%	where:
%	   DINAPITU PASARAN = combination of day names in the Gregorian/Islamic
%	   7-day week and Javanese 5-day week, i.e., the "Weton" (Ngoko/Krama)
%	   WUKU = Javanese/Balinese 7-day week name (30 different)
%	   DINA = day number in the Javanese month (1 to 29 or 30)
%	   WULAN = Javanese month name
%	   T = Javanese year number (starts on 1555)
%	   TAUN = Javanese year name (8 different, 12-Wulan cycle)
%	   WINDU = Javanese "decade" name (4 different, 8-Taun cycle)
%	   KURUP = Javanese "century" name (7 different, 120-Taun cycle)
%	   DINA MULYA = "noble day" name (if necessary)
%
%	WETON(YEAR,MONTH,DAY) returns the javanese date corresponding to
%	YEAR-MONTH-DAY in the Gregorian calendar.
%
%	WETON(YEAR,MONTH,DAY,N) returns the list of your N first javanese
%	birthdays (from the 35-day "Weton" cycle). Example: if you are born
%	on Dec 3, 1968 then
%	   weton(1968,12,3,10)
%	returns your 10 first Wetons. Thanks to the Matlab flexibility,
%	   weton(1968,12,3+35*(0:10))
%	will do the same job...
%
%	WETON(T) uses date T which can be Matlab date scalar, vector, matrix
%	or any valid string for DATENUM function. Examples:
%	   weton(719135)
%	   weton('3-Dec-1968')
%	   weton([1968,12,3])
%	all return the string
%	   'Selasa Kliwon/Asih Julungwangi 12 Pasa 1900 Eh Adi Arbangiyah, 3 Desember 1968'
%
%
%	-- Calendar mode --
%
%	WETON(YEAR,MONTH) returns a javanese calendar for YEAR and MONTH in a
%	table combining the 5-day "Pasaran" cycle and 7-day Gregorian week.
%	Example: weton(1994,4) returns the following:
%
%	------------------ WETONAN BULAN APRIL 1994 ------------------
%	Awal:  Jemuwah Kliwon/Asih Sungsang 19 Sawal 1926 J Sancaya Salasiyah,  1 April 1994
%	Akhir: Setu Wage/Cemeng Mandasiya 19 Dulkangidah 1926 J Sancaya Salasiyah, 30 April 1994
%	------------------------------------------------------------------
%            Senin  Selasa    Rebo   Kemis Jemuwah    Setu    Akad
%	   Pon      04      19       -      14      29      09      24
%	  Wage      25      05      20       -      15      30      10
%	Kliwon      11      26      06      21      01      16       -
%	  Legi       -      12      27      07      22      02      17
%	Pahing      18       -      13      28      08      23      03
%
%	where "Awal:" is the first day of the month, "Akhir:" the last one.
%
%	-- Search mode --
%
%	WETON(REGEXP) will search the next match REGEXP into the full Javanese
%	date string from today (limited to the next 8 years).
%
%	WETON(YEAR,REGEXP) will search the match REGEXP into the current YEAR.
%
%	WETON(T,REGEXP) searches the string match REGEXP into date vector T
%	(DATENUM format).
%
%
%	W = WETON(...) returns a structure W with corresponding fields instead
%	of displaying strings. To see the field names, try
%	   disp(weton)
%
%	Examples:
%	   W = weton(2016,1,1:35);
%	   datestr(cat(1,W.date))	% use Matlab datenum
%	   cat(1,char(W.weton))		% display full date strings
%	   weton('30 rejeb')		% looks for the next "30 Rejeb"
%
%
%	Author: Mas François Beauducel
%
%	References:
%	   https://id.wikipedia.org/wiki/Kalender_Jawa
%
%	Created: 1999-01-27 (Rebo Pahing), in Paris (France)
%	Updated: 2019-01-15 (Selasa Kliwon)

%	Copyright (c) 2019, François Beauducel, covered by BSD License.
%	All rights reserved.
%
%	Redistribution and use in source and binary forms, with or without
%	modification, are permitted provided that the following conditions are
%	met:
%
%	   * Redistributions of source code must retain the above copyright
%	     notice, this list of conditions and the following disclaimer.
%	   * Redistributions in binary form must reproduce the above copyright
%	     notice, this list of conditions and the following disclaimer in
%	     the documentation and/or other materials provided with the distribution
%
%	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
%	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
%	IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
%	ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
%	LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
%	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
%	SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
%	INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
%	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
%	ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
%	POSSIBILITY OF SUCH DAMAGE.

global pasaran minggu

% Pasaran = 5 day-names of javanese pasaran
pasaran = {'Pon/Petak','Wage/Cemeng','Kliwon/Asih','Legi/Manis','Pahing/Pahit'};

% Minggu = 7 day-names of gregorian week
minggu = {'Senen','Selasa','Rebo','Kemis','Jemuwah','Setu','Akad'};

if nargin > 4
error('Too many input arguments.');
end

if nargin == 2
% if year is a 4-digit value, supposes it is not a datenum date
if year < 10000 && ischar(month)
year = datenum(year,1,1:366);
end
end

if nargin > 2 && ~isnumeric(day)
error('DAY argument must be numeric.')
end

if nargin == 4 && (~isnumeric(n) ||  numel(n) > 1)
error('N argument must be scalar.')
end

search = '';

switch nargin
case 1
try
dt = datenum(year);
catch
if ischar(year)
dt = now + (1:366*8);	% will search from today to 8 years ahead
search = year;
else
error('T is not a valid date (DATENUM).')
end
end
case 2
if ischar(month)
search = month;
dt = datenum(year);
else
% calculates the number of days in month using DATEVEC
ct = datevec(datenum(year,month,1:31));
year = ct(1,1);
month = ct(1,2);
day = max(ct(:,3));
dt = datenum(year,month,1:day);
end
case 3
dt = datenum(year,month,day);
case 4
dt = datenum(year,month,day) + 35*(0:n)';
otherwise
dt = now;
end
dt = floor(dt);

% --- computes parameters for all dates
for i = 1:numel(dt)
X(i) = wetonan(dt(i),pasaran,minggu);
end
s = cat(1,{X.weton});

if ~isempty(search)
X = X(~cellfun(@isempty,regexp(s,search,'ignorecase')));
if isempty(X)
s = 'no match.';
else
% looks only for the first match
if nargin == 1
X = X(1);
s = X(1).weton;
else
s = cat(1,{X.weton});
end
end
end

if nargin == 2 && isempty(search)
s = cell(10,1);

kal = cell(5,7);
kal(:) = {' -'};
for i = 1:day
kal{mod(dt(i)+2,5)+1,mod(dt(i)+4,7)+1} = sprintf('%02d',i);
end
s{1} = sprintf('--------------------- WETONAN BULAN %s %d ---------------------', ...
upper(X(1).month),X(1).year);
s{2} = sprintf('Awal:  %s',X(1).weton);
s{3} = sprintf('Akhir: %s',X(end).weton);
s{4} = sprintf('%s',repmat('-',68,1));
s{5} = sprintf('%s %s',repmat(' ',1,15),[char(minggu),32*ones(7,1)]');
for i = 1:5
s{5+i} = [sprintf('%12s',pasaran{i}),sprintf('%8s',kal{i,:})];
end
end


if nargout == 0
disp(char(s));
else
varargout{1} = X;
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function X = wetonan(dt,pasaran,minggu)
%WETONAN Computes Weton from date DT

% origin date = 1 Sura 1555 Alip Kuntara
taun0 = 1555;
t0 = datenum(1633,7,8);

% 24 March 1936 = Selasa Pon 1 Sura 1867 Alip Adi Salasiyah (ASAPON)
%+ start a change of Wulan length in each Tahun
t1 = datenum(1936,3,24);

% Wuku = 30 week names in the Javanese/Balinese calendar
wuku = {'Sinta','Landep','Wukir','Kurantil','Tolu','Gumbreg', ...
'Warigalit','Warigagung','Julungwangi','Sungsang','Galungan','Kuningan', ...
'Langkir','Mandasiya','Julungpujut','Pahang','Kuruwelut','Marakeh', ...
'Tambir','Medangkungan','Maktal','Wuye','Manahil','Prangbakat', ...
'Bala','Wugu','Wayang','Kulawu','Dukut','Watugunung'};

% Bulan = Gregorian month names
bulan = {'Januari','Februari','Maret','April','Mei','Juni', ...
'Juli','Agustus','September','Oktober','November','Desember'};

% Wulan = Javanese month names (1/12 of Moon year)
wulan = {'Sura','Sapar','Mulud','Bakdamulud','Jumadilawal','Jumadilakhir', ...
'Rejeb','Ruwah','Pasa','Sawal','Sela','Besar'};

% Tahun = Moon year, alternate 354 and 355-day length (depending on wulan Besar length)
taun = {'Alip','Ehe','Jimawal','Je','Dal','Be','Wawu','Jimakhir'};

% Windu = 8-taun cycle = 81-wetonan = 2835-day
windu = {'Adi','Kuntara','Sengara','Sancaya'};

% for each Windu, determine a vector for number of days per Tahun
w354 = 29+[1,0,1,0,1,0,1,0,1,0,1,0]';	% Tahun 1,3,5,6,7
w355 = 29+[1,0,1,0,1,0,1,0,1,0,1,1]';	% Tahun 2,4,8
wdal = 29+[1,1,0,0,0,0,1,0,1,0,1,1]';	% Tahun 5 (Dal) before 1936-2-26

% Windu matrix (size 12x8) of Wulan x Tahun
hw0 = [w354,w355,w354,w355,wdal,w354,w354,w355];	% before 1936-3-26
hw1 = [w354,w355,w354,w355,w354,w354,w354,w355];	% since 1936-3-26

% Kurup = 120-taun cycle = 15-windu = 42524-day (1 day must be substracted)
kurup = {'Jamingiyah','Kamsiyah','Arbangiyah','Salasiyah','Isneniyah','Akadiyah','Sabtiyah'};

if any(dt < t0)
warning('Some dates are unvalid (before %s)',datestr(t0));
end

ct = datevec(dt);
dti = dt - t0;
dti = dti + floor(dti/(15*81*7*5 - 1));
% relative date into the Windu
dw = mod(dti,81*7*5) + 1;
if dt < t1
% finds the Wulan and Tahun into the table of Windu
kk = find(cumsum(hw0(:)) >= dw,1);
[wulan_index,taun_index] = ind2sub(size(hw0),kk);
% computes the day number
if kk == 1
dina = dw;
else
dina = dw - sum(hw0(1:(kk-1)));
end
else
% finds the Wulan and Tahun into the table of Windu
kk = find(cumsum(hw1(:)) >= dw,1);
[wulan_index,taun_index] = ind2sub(size(hw1),kk);
% computes the day number
if kk == 1
dina = dw;
else
dina = dw - sum(hw1(1:(kk-1)));
end
end
% t: taun number
t = floor(dti/(81*7*5))*8 + taun_index + taun0 - 1;
pasaran_index = mod(dt + 2,5) + 1;
minggu_index = mod(dt + 4,7) + 1;
wuku_index = mod(floor((dt - 2)/7) + 25,30) + 1;
windu_index = mod(floor(dti/(81*7*5)) + 1,4) + 1;
kurup_index = mod(floor(dti/(15*81*7*5)),7) + 1;

X.pasaran = pasaran{pasaran_index};
X.dina = minggu{minggu_index};
X.wuku = wuku{wuku_index};
X.date = dt;
X.d = dina;
X.wulan = wulan{wulan_index};
X.t = t;
X.taun = taun{taun_index};
X.windu = windu{windu_index};
X.kurup = kurup{kurup_index};
X.day = ct(3);
X.month = bulan{ct(2)};
X.year = ct(1);

% "Dina Mulya": noble days
mulya = '';
if dina == 1 && wulan_index == 1
mulya = 'Siji Sura';	% 1 Sura (new year)
end
if taun_index == 1 && wulan_index == 3 && pasaran_index == 2
mulya = 'Aboge';	% Alip Rebo Wage
end
if taun_index == 5 && minggu_index == 6 && pasaran_index == 4
mulya = 'Daltugi';	% Dal Setu Legi
end
if wuku_index == 12 && minggu_index == 6 && pasaran_index == 3
mulya = 'Kuningan';	% Setu Kliwon Kuningan
end
if wuku_index == 29 && minggu_index == 2 && pasaran_index == 3
mulya = 'Hanggara Asih';	% Selasa Kliwon Dukut
end
if wuku_index == 30 && minggu_index == 5 && pasaran_index == 3
mulya = 'Dina Mulya';	% Jumaat Kliwon Watugunung
end
if minggu_index == 5 && pasaran_index == 4
mulya = 'Dina Purnama';
end
X.mulyo = mulya;

ss = sprintf('%s %s %s %2d %1s %4d %s %s %s, %2d %s %4d', ...
X.dina,X.pasaran,X.wuku, dina, X.wulan, t, X.taun, ...
X.windu, X.kurup, X.day, X.month, X.year);
if ~isempty(mulya)
ss = sprintf('%s (%s)',ss,upper(mulya));
end
X.weton = ss;
 */
