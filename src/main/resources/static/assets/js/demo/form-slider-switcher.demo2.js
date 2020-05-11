/*
Template Name: Color Admin - Responsive Admin Dashboard Template build with Twitter Bootstrap 3 & 4
Version: 4.1.0
Author: Sean Ngu
Website: http://www.seantheme.com/color-admin-v4.1/admin/
*/

var renderSwitcher = function(parentElement) {
	var findElement = '[data-render=switchery]';
	if (parentElement) {
		findElement = parentElement + " " + findElement;
	}
	
    if ($(findElement).length !== 0) {
        $(findElement).each(function() {
            var themeColor = COLOR_GREEN;
            if ($(this).attr('data-theme')) {
                switch ($(this).attr('data-theme')) {
                    case 'red':
                        themeColor = COLOR_RED;
                        break;
                    case 'blue':
                        themeColor = COLOR_BLUE;
                        break;
                    case 'purple':
                        themeColor = COLOR_PURPLE;
                        break;
                    case 'orange':
                        themeColor = COLOR_ORANGE;
                        break;
                    case 'black':
                        themeColor = COLOR_BLACK;
                        break;
                }
            }
            var option = {};
            	option.size = 'small'
                option.color = themeColor;
                option.secondaryColor = ($(this).attr('data-secondary-color')) ? $(this).attr('data-secondary-color') : '#dfdfdf';
                option.className = ($(this).attr('data-classname')) ? $(this).attr('data-classname') : 'switchery';
                option.disabled = ($(this).attr('data-disabled')) ? true : false;
                option.disabledOpacity = ($(this).attr('data-disabled-opacity')) ? parseFloat($(this).attr('data-disabled-opacity')) : 0.5;
                option.speed = ($(this).attr('data-speed')) ? $(this).attr('data-speed') : '0.5s';
            var switchery = new Switchery(this, option);
        });
    }
};

var checkSwitcherState = function() {
    $(document).on('click', '[data-click="check-switchery-state"]', function() {
        alert($('[data-id="switchery-state"]').prop('checked'));
    });
    $(document).on('change', '[data-change="check-switchery-state-text"]', function() {
        $('[data-id="switchery-state-text"]').text($(this).prop('checked'));
    });
};

var FormSliderSwitcher = function () {
	"use strict";
    return {
        //main function
        init: function (parentElement) {
            // switchery
            renderSwitcher(parentElement);
            //checkSwitcherState();
        }
    };
}();