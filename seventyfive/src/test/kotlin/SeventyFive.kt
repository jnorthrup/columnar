#!/usr/bin/env kscript

import org.junit.jupiter.api.Test
class SeventyFiveTest{
// define kotlin options
//%use "columnar:cursor:1.0-SNAPSHOT"  //someday


//   Table of contents
@Test
fun `test  How to import Columnar and check the version? `(){}

@Test fun `test  How to create a series from a list, numpy array and dict? `(){}

@Test fun `test  How to convert the index of a series into a column of a dataframe? `(){}

@Test fun `test  How to combine many series to form a dataframe? `(){}

@Test fun `test  How to assign name to the series index? `(){}

@Test fun `test  How to get the items of series A not present in series B? `(){}

@Test fun `test  How to get the items not common to both series A and series B? `(){}

@Test fun `test  How to get the minimum, 25th percentile, median, 75th, and max of a numeric series? `(){}

@Test fun `test  How to get frequency counts of unique items of a series? `(){}

@Test fun `test  How to keep only top 2 most frequent values as it is and replace everything else as  Other ? `(){}

@Test fun `test  How to bin a numeric series to 10 groups of equal size? `(){}

@Test fun `test  How to convert a numpy array to a dataframe of given shape? `(){}

@Test fun `test  How to find the positions of numbers that are multiples of 3 from a series? `(){}

@Test fun `test  How to extract items at given positions from a series? `(){}

@Test fun `test  How to stack two series vertically and horizontally ? `(){}

@Test fun `test  How to get the positions of items of series A in another series B? `(){}

@Test fun `test  How to compute the mean squared error on a truth and predicted series? `(){}

@Test fun `test  How to convert the first character of each element in a series to uppercase? `(){}

@Test fun `test  How to calculate the number of characters in each word in a series? `(){}

@Test fun `test  How to compute difference of differences between consequtive numbers of a series? `(){}

@Test fun `test  How to convert a series of date-strings to a timeseries? `(){}

@Test fun `test  How to get the day of month, week number, day of year and day of week from a series of date strings? `(){}

@Test fun `test  How to convert year-month string to dates corresponding to the 4th day of the month? `(){}

@Test fun `test  How to filter words that contain atleast 2 vowels from a series? `(){}

@Test fun `test  How to filter valid emails from a series? `(){}

@Test fun `test  How to get the mean of a series grouped by another series? `(){}

@Test fun `test  How to compute the euclidean distance between two series? `(){}

@Test fun `test  How to find all the local maxima (or peaks) in a numeric series? `(){}

@Test fun `test  How to replace missing spaces in a string with the least frequent character? `(){}

@Test fun `test  How to create a TimeSeries starting 2000 01 01 and 10 weekends  _saturdays_  after that having random numbers as values? `(){}

@Test fun `test  How to fill an intermittent time series so all missing dates show up with values of previous non-missing date? `(){}

@Test fun `test  How to compute the autocorrelations of a numeric series? `(){}

@Test fun `test  How to import only every nth row from a csv file to create a dataframe? `(){}

@Test fun `test  How to change column values when importing csv to a dataframe? `(){}

@Test fun `test  How to create a dataframe with rows as strides from a given series? `(){}

@Test fun `test  How to import only specified columns from a csv file? `(){}

@Test fun `test  How to get the nrows ncolumns  datatype  summary stats of each column of a dataframe? Also get the array and list equivalent`(){}

@Test fun `test  How to extract the row and column number of a particular cell with given criterion? `(){}

@Test fun `test  How to rename a specific columns in a dataframe? `(){}

@Test fun `test  How to check if a dataframe has any missing values? `(){}

@Test fun `test  How to count the number of missing values in each column? `(){}

@Test fun `test  How to replace missing values of multiple numeric columns with the mean? `(){}

@Test fun `test  How to use apply function on existing columns with global variables as additional arguments? `(){}

@Test fun `test  How to select a specific column from a dataframe as a dataframe instead of a series? `(){}

@Test fun `test  How to change the order of columns of a dataframe? `(){}

@Test fun `test  How to set the number of rows and columns displayed in the output? `(){}

@Test fun `test  How to format or suppress scientific notations in a Columnar dataframe? `(){}

@Test fun `test  How to format all the values in a dataframe as percentages? `(){}

@Test fun `test  How to filter every nth row in a dataframe? `(){}

@Test fun `test  How to create a primary key index by combining relevant columns? `(){}

@Test fun `test  How to get the row number of the nth largest value in a column? `(){}

@Test fun `test  How to find the position of the nth largest value greater than a given value? `(){}

@Test fun `test  How to get the last n rows of a dataframe with row sum gt 100? `(){}

@Test fun `test  How to find and cap outliers from a series or dataframe column? `(){}

@Test fun `test  How to reshape a dataframe to the largest possible square after removing the negative values? `(){}

@Test fun `test  How to swap two rows of a dataframe? `(){}

@Test fun `test  How to reverse the rows of a dataframe? `(){}

@Test fun `test  How to create one-hot encodings of a categorical variable (dummy variables)? `(){}

@Test fun `test  Which column contains the highest number of row-wise maximum values? `(){}

@Test fun `test  How to create a new column that contains the row number of nearest column by euclidean distance? `(){}

@Test fun `test  How to know the maximum possible correlation value of each column against other columns? `(){}

@Test fun `test  How to create a column containing the minimum by maximum of each row? `(){}

@Test fun `test  How to create a column that contains the penultimate value in each row? `(){}

@Test fun `test  How to normalize all columns in a dataframe? `(){}

@Test fun `test  How to compute the correlation of each row with the suceeding row? `(){}

@Test fun `test  How to replace both the diagonals of dataframe with 0? `(){}

@Test fun `test  How to get the particular group of a groupby dataframe by key? `(){}

@Test fun `test  How to get the th largest value of a column when grouped by another column? `(){}

@Test fun `test  How to compute grouped mean on Columnar dataframe and keep the grouped column as another column (not index)? `(){}

@Test fun `test  How to join two dataframes by 2 columns so they have only the common rows? `(){}

@Test fun `test  How to remove rows from a dataframe that are present in another dataframe? `(){}

@Test fun `test  How to get the positions where values of two columns match? `(){}

@Test fun `test  How to create lags and leads of a column in a dataframe? `(){}

@Test fun `test  How to get the frequency of unique values in the entire dataframe? `(){}

@Test fun `test  How to split a text column into two separate columns? `(){}
}